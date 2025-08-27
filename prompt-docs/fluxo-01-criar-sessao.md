# FLUXO: Criar Sessão do Usuário

## 📋 Visão Geral
- **Trigger**: Chamada API REST pelo portal
- **Objetivo**: Validar usuário, criar sessão completa com relacionamentos e permissões gerais
- **Microserviço**: `fidc-auth`
- **Endpoint**: `POST /sessions`

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
- `location-timestamp` (timestamp da captura da localização - se não informado, será salvo como nulo)
- `x-correlation-id` (gerado automaticamente se ausente)

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

### 1. Validações de Entrada
* **Rate limiting:** Verificar limites por IP e User-Agent
* **Se limite excedido:** Retornar erro 429 "Rate limit excedido"
* **Headers obrigatórios:** partner, user-agent, channel, fingerprint, latitude, longitude, location-accuracy, location-timestamp
* **Se headers ausentes:** Retornar erro 400 "Headers obrigatórios ausentes"
* **JWT:** Validar assinatura usando estratégia de fallback (cache Redis → AWS → FidcPassword)
* **Se JWT inválido:** Retornar erro 400 "Token JWT inválido"
* **Extrair CPF:** Deve ter 11 dígitos numéricos
* **Se CPF inválido:** Retornar erro 400 "CPF inválido"

### 2. Validação de Consistência e Invalidação de Sessão Anterior
* **Buscar controle de usuário no PostgreSQL:** tabela `user_session_control` usando CPF + partner
* **Se encontrar registro:**
   * **Verificar consistência entre cache e histórico:**
      * Buscar última sessão do histórico em `session_access_history`
      * Se `current_session_id` ≠ `session_id` do último histórico:
         * Logar inconsistência detectada
         * Corrigir `current_session_id` automaticamente
   * **Se is_active = true (sessão anterior ativa):**
      * Buscar sessão anterior no Redis: `session:{current_session_id}`
      * Se sessão existe no Redis → Remover do Redis
      * Se erro ao remover do Redis → Retornar erro 500 genérico

### 3. Busca de Dados do Usuário (UserManagement)
* **Chamar UserManagement:** GET /users com headers partner e cpf
* **Se erro na integração:** Retornar erro 503 "Serviço temporariamente indisponível"
* **Se usuário não encontrado:** Retornar erro 404 "Usuário não encontrado"
* **Se sucesso:** Extrair userInfo, fund e relationshipList

### 4. Busca de Permissões Gerais (FidcPermission)
* **Chamar FidcPermission:** GET /permissions com headers partner e cpf (SEM relationshipId)
* **Se erro na integração:** Retornar erro 503 "Serviço temporariamente indisponível"
* **Se sem permissões:** Continuar com array vazio (usuário pode não ter permissões gerais)
* **Se sucesso:** Extrair permissões gerais

### 5. Geração de Identificadores da Sessão
* **Gerar sessionId:** UUID único
* **Gerar sessionSecret:** Hash único para assinatura do AccessToken desta sessão
* **Definir expiração:** Timestamp atual + 30 minutos

### 6. Persistência Atômica da Sessão
**Operação transacional no PostgreSQL + Redis:**
* **Atualizar/Inserir em** `user_session_control`:
   * Se é primeiro acesso: `first_access_at = NOW()`
   * Se não é primeiro acesso: `previous_access_at = last_access_at`
   * Sempre: `last_access_at = NOW()`, `current_session_id = sessionId`, `is_active = true`
   * Se erro no PostgreSQL → Retornar erro 500 genérico
* **Inserir em** `session_access_history`:
   * Todos os dados completos da sessão (occurred_at, ip_address, user_agent, latitude, longitude, location_accuracy, location_timestamp)
   * Se erro no PostgreSQL → Retornar erro 500 genérico
* **Salvar sessão no Redis:** `session:{sessionId}`
   * Incluir sessionSecret nos dados
   * TTL Redis: 30 minutos
   * Se erro ao salvar no Redis → Retornar erro 500 genérico

### 7. Geração do AccessToken
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

### 8. Resposta Final
* **Retornar dados completos:** userInfo, fund, relationshipList, permissions, accessToken
* **Não incluir:** relationshipsSelected (será preenchido apenas no próximo fluxo)
* **Log INFO:** Sessão criada com sucesso

## 🔧 Integrações e Configurações:

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
        "name": "Millenium Inc",
        "status": "ACTIVE",
        "contractNumber": "378192372163682"
      }
    ]
  }
  ```

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

### Configurações:
```kotlin
@ConfigurationProperties("fidc.auth")
data class FidcAuthConfig(
    val userManagement: ApiConfig = ApiConfig(),
    val fidcPermission: ApiConfig = ApiConfig(),
    val session: SessionConfig = SessionConfig()
) {
    data class ApiConfig(
        val baseUrl: String = "http://localhost:8080",
        val timeoutSeconds: Int = 10,
        val retryAttempts: Int = 3
    )
    
    data class SessionConfig(
        val ttlMinutes: Int = 30,
        val secretLength: Int = 36
    )
}
```

## 📊 Observabilidade:
- **Logs INFO**: Sessão criada, sessão anterior invalidada, integrações bem-sucedidas
- **Logs WARN**: Inconsistência detectada entre cache e banco, permissões vazias
- **Logs ERROR**: Integrações falharam, erro de persistência, Redis indisponível
- **Logs DEBUG**: 
  - Dados encontrados nas integrações (sem dados sensíveis)
  - Estado da sessão anterior (se encontrada)
  - Operações de banco de dados e Redis
- **Métricas**: 
  - Contador de sessões criadas por partner
  - Latência das integrações (UserManagement, FidcPermission)
  - Taxa de sessões anteriores encontradas vs novas
  - Contador de inconsistências detectadas entre cache e banco