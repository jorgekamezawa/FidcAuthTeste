# FLUXO: Criar Sessão do Usuário

## 📋 Visão Geral
- **Trigger**: Chamada API REST pelo portal
- **Objetivo**: Autenticar usuário via JWT, invalidar sessão anterior, buscar dados do usuário e permissões gerais, criar nova sessão com AccessToken
- **Microserviço**: `fidc-auth`
- **Endpoint**: `POST /v1/sessions`

## 🔄 Contrato da API

### Headers Obrigatórios:
- `partner` (prevcom, caio, etc.)
- `user-agent` (para rate limiting)
- `channel` (WEB, MOBILE, etc.)
- `fingerprint` (identificação do dispositivo)

### Headers Opcionais:
- `latitude` (localização GPS - se não informado, será salvo como nulo)
- `longitude` (localização GPS - se não informado, será salvo como nulo)
- `location-accuracy` (precisão da localização em metros - se não informado, será salvo como nulo)
- `location-timestamp` (timestamp da captura da localização ISO format - se não informado, será salvo como nulo)
- `x-correlation-id` (gerado automaticamente pelo CorrelationIdFilter se ausente)

### Headers Automáticos (para rate limiting):
- `x-forwarded-for` ou `remote-addr` (IP do cliente)

### Request Body:
```json
{
  "signedData": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcGYiOiIxMjM0NTY3ODkwMSJ9.signature"
}
```

**Payload decodificado do JWT:**
```json
{
  "cpf": "12345678901"
}
```

### Response (Sucesso):
```json
{
  "userInfo": {
    "cpf": "12345678901",
    "fullName": "João Silva Santos",
    "email": "joao.silva@email.com",
    "birthDate": "1985-03-15",
    "phoneNumber": "+5511999887766"
  },
  "fund": {
    "id": "CRED001",
    "name": "Prevcom RS",
    "type": "PREVIDENCIA"
  },
  "relationshipList": [
    {
      "id": "REL001",
      "type": "PLANO_PREVIDENCIA",
      "name": "Plano Previdência Básico",
      "status": "ACTIVE",
      "contractNumber": "378192372163682"
    },
    {
      "id": "REL002",
      "name": "Plano Previdência Premium",
      "status": "ACTIVE",
      "contractNumber": "4353453456475465"
    }
  ],
  "permissions": [
    "VIEW_PROFILE",
    "VIEW_STATEMENTS",
    "VIEW_PLAN_DETAILS",
    "VIEW_CONTRIBUTIONS",
    "DOWNLOAD_DOCUMENTS",
    "UPDATE_PERSONAL_DATA",
    "REQUEST_PORTABILITY"
  ],
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Response (Erro):
```json
{
  "timestamp": "2025-08-18T14:45:32",
  "status": 400,
  "error": "Bad Request",
  "message": "Token JWT inválido",
  "path": "/sessions"
}
```

### Códigos de Erro:
- **400**: JWT inválido, dados inválidos, headers obrigatórios ausentes
- **404**: Usuário não encontrado no UserManagement
- **429**: Rate limit excedido
- **500**: Erro interno (integrações, banco de dados)
- **503**: Serviços externos indisponíveis (UserManagement, FidcPermission)

## 🛡️ Política de Rate Limiting:
- **Por IP**: 20 req/min, 100 req/hora
- **Por User-Agent**: 40 req/min, 200 req/hora
- **Burst**: Até 5 req consecutivas

## 📋 Regras de Negócio:

### 1. Validações Simples de Entrada
* **Rate limiting:** Verificar limites por IP e User-Agent
* **Se limite excedido:** Retornar erro 429 "Rate limit excedido"
* **Headers obrigatórios:** Validar presença de partner, user-agent, channel, fingerprint
* **Se headers ausentes:** Retornar erro 400 "Headers obrigatórios ausentes"
* **Channel:** Validar se o valor informado é um canal suportado (WEB, MOBILE, etc.)
* **Se Channel inválido:** Retornar erro 400 "Channel '[valor]' é incorreto. Valores aceitos: [lista]"

### 2. Autenticação JWT e Extração de Dados
* **Validar JWT:** Verificar assinatura do token no campo signedData
* **Se JWT inválido:** Retornar erro 400 "Token JWT inválido"
* **Extrair CPF:** Obter CPF do usuário a partir do payload do token
* **Se CPF ausente/inválido:** Retornar erro 400 "Dados de usuário inválidos no token"

### 3. Invalidação de Sessão Anterior
* **Buscar sessão anterior:** Verificar se usuário já possui sessão ativa para o partner
* **Se sessão anterior encontrada:**
   * **Remover sessão do cache:** Invalidar sessão ativa no Redis
   * **Atualizar controle de sessão:** Marcar sessão como inativa no banco de dados
   * **Log:** Registrar invalidação da sessão anterior
* **Se erro ao invalidar:** Retornar erro 500 "Erro interno do servidor"
* **Se nenhuma sessão anterior:** Prosseguir normalmente

### 4. Busca de Dados do Usuário
* **Chamar UserManagement:** GET /users com headers partner e cpf
* **Se erro na integração:** Retornar erro 503 "Serviço temporariamente indisponível"
* **Se usuário não encontrado:** Retornar erro 404 "Usuário não encontrado"
* **Se sucesso:** Obter dados pessoais, informações do fundo e lista de relacionamentos

### 5. Busca de Permissões Gerais
* **Chamar FidcPermission:** GET /permissions com headers partner e cpf (SEM relationshipId)
* **Se erro na integração:** Retornar erro 503 "Serviço temporariamente indisponível"
* **Se sem permissões:** Continuar com array vazio (usuário pode não ter permissões gerais)
* **Se sucesso:** Obter lista de permissões gerais do usuário

### 6. Geração de Identificadores da Sessão
* **Gerar sessionId:** UUID único
* **Gerar sessionSecret:** Hash único para assinatura do AccessToken desta sessão
* **Definir expiração:** Timestamp atual + 30 minutos

### 7. Persistência Atômica da Sessão
**Operação transacional:**
* **Controle de Sessão:**
   * Atualizar ou criar registro de controle do usuário (CPF + partner)
   * Registrar nova sessão como ativa com timestamps
   * Se erro no banco → Retornar erro 500
* **Histórico de Acesso:**
   * Registrar acesso com dados da requisição (IP, user-agent, localização)
   * Tratar dados opcionais de localização (latitude, longitude, etc.)
   * Se erro no banco → Retornar erro 500
* **Cache da Sessão:**
   * Salvar sessão completa no Redis com TTL configurado
   * Se erro no cache → Retornar erro 500

### 8. Geração do AccessToken
* **Assinar JWT:** Usar sessionSecret gerado especificamente para esta sessão
* **Claims:**
  ```json
  {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "exp": 1692360000
  }
  ```
* **Algoritmo:** HMAC-SHA256
* **TTL:** 30 minutos (mesma duração da sessão)

### 9. Resposta Final
* **Preparar resposta:** Organizar dados do usuário, fundo, relacionamentos e permissões
* **Retornar dados completos:** userInfo, fund, relationshipList, permissions, accessToken
* **Não incluir:** relationshipSelected (será definido apenas no fluxo de seleção)
* **Log:** Registrar sucesso na criação da sessão

## 🔧 Integrações Externas:

### UserManagement API
- **Base URL**: http://localhost:8081 (dev), http://localhost:8081 (uat), http://localhost:8081 (prod)
- **Endpoint**: `GET /users`
- **Headers**: `partner`, `cpf`
- **Response**:
  ```json
  {
    "userInfo": {
      "cpf": "12345678901",
      "email": "joao.silva@gmail.com",
      "fullName": "João Silva Santos",
      "birthDate": "1985-03-15",
      "phoneNumber": "+5511957753776"
    },
    "fund": {
      "id": "CRED001",
      "name": "PREVCOM",
      "type": "CLEAN"
    },
    "relationshipList": [
      {
        "id": "REL001",
        "type": "PLANO_PREVIDENCIA",
        "name": "Millenium Inc",
        "status": "ACTIVE",
        "contractNumber": "378192372163682"
      }
    ]
  }
  ```
- **Tratamento de Erro**: Se indisponível → Retornar erro 503 "Serviço temporariamente indisponível"
- **Usuário não encontrado**: Retornar erro 404 "Usuário não encontrado"

### FidcPermission API
- **Base URL**: http://localhost:8082 (dev), http://localhost:8082 (uat), http://localhost:8082 (prod)
- **Endpoint**: `GET /permissions`
- **Headers**: `partner`, `cpf` (SEM relationshipId para permissões gerais)
- **Response**:
  ```json
  {
    "permissions": ["VIEW_CONTRACTS", "CREATE_SIMULATION"]
  }
  ```
- **Tratamento de Erro**: Se indisponível → Retornar erro 503 "Serviço temporariamente indisponível"
- **Sem permissões**: Retornar array vazio (comportamento normal)

### Redis Session Storage
- **Chave**: `session:{sessionId}`
- **TTL**: 30 minutos
- **Estrutura**:
  ```json
  {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "createdAt": "2025-07-01T12:47:21",
    "updatedAt": "2025-07-01T12:47:21",
    "partner": "prevcom",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "channel": "WEB",
    "fingerprint": "abc123def456",
    "sessionSecret": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "userInfo": {
      "cpf": "12345678901",
      "fullName": "João Silva Santos",
      "email": "joao.silva@email.com",
      "birthDate": "1985-03-15",
      "phoneNumber": "+5511999887766"
    },
    "fund": {
      "id": "CRED001",
      "name": "Prevcom RS",
      "type": "PREVIDENCIA"
    },
    "relationshipList": [
      {
        "id": "REL001",
        "type": "PLANO_PREVIDENCIA",
        "name": "Plano Previdência Básico",
        "status": "ACTIVE",
        "contractNumber": "378192372163682"
      }
    ],
    "relationshipsSelected": null,
    "permissions": [
      "VIEW_PROFILE",
      "VIEW_STATEMENTS",
      "VIEW_PLAN_DETAILS"
    ]
  }
  ```

### PostgreSQL Tables
```sql
CREATE TABLE user_session_control (
    id BIGSERIAL PRIMARY KEY,
    cpf VARCHAR(11) NOT NULL,
    partner VARCHAR(100) NOT NULL,
    current_session_id UUID,
    is_active BOOLEAN DEFAULT false,
    first_access_at TIMESTAMP,
    previous_access_at TIMESTAMP,
    last_access_at TIMESTAMP,
    UNIQUE(cpf, partner)
);

CREATE TABLE session_access_history (
    id BIGSERIAL PRIMARY KEY,
    user_session_control_id BIGINT REFERENCES user_session_control(id),
    session_id UUID NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    ip_address INET,
    user_agent TEXT,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    location_accuracy INTEGER,
    location_timestamp TIMESTAMP
);
```

### Política de Integração Global:
- **Timeout**: 10 segundos para todas as integrações
- **Retry**: 3 tentativas com backoff exponencial

### Configurações do Sistema:
- **TTL da Sessão**: 30 minutos (configurado via properties)
- **Segurança**: Geração de segredo único por sessão para assinatura JWT
- **Rate Limiting**: Limites por IP e User-Agent conforme política definida
- **Timeout Integrações**: 10 segundos com retry automático

## 📊 Observabilidade e Logs:
- **Logs INFO**: 
  - Início do processo de criação de sessão
  - Sucesso na criação da sessão
  - Invalidação de sessão anterior quando necessário
- **Logs WARN**: 
  - Channel inválido informado na requisição
  - Erros de negócio e validação
  - Dados opcionais inválidos (IP, timestamp de localização)
- **Logs ERROR**: 
  - Falhas em integrações externas
  - Erros de persistência (banco, cache)
  - Erros inesperados no processamento
- **Logs DEBUG**: 
  - Detalhes do processo de invalidação de sessão anterior
  - Confirmações de operações de persistência
  - Estados intermediários do fluxo

**Correlation ID**: Automaticamente incluído em todos os logs pelo filtro do sistema.