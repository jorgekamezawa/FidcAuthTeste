# FLUXO: Selecionar Relacionamento

## 📋 Visão Geral
- **Trigger**: Chamada API REST pelo portal
- **Objetivo**: Selecionar relacionamento específico, buscar permissões contextuais e atualizar sessão
- **Microserviço**: `fidc-auth`
- **Endpoint**: `PATCH /sessions/relationship`

## 📄 Contrato da API

### Headers Obrigatórios:
- `authorization` (Bearer {accessToken} do FLUXO 1)

### Headers Opcionais:
- `x-correlation-id` (gerado automaticamente se ausente)

### Headers Automáticos (para rate limiting):
- `x-forwarded-for` ou `remote-addr` (IP do cliente)
- `user-agent` (identificação do browser/client)

### Request Body:
```json
{
  "relationshipId": "REL001"
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
  "relationshipSelected": {
    "id": "REL001",
    "type": "PLANO_PREVIDENCIA",
    "name": "Plano Previdência Básico",
    "status": "ACTIVE",
    "contractNumber": "378192372163682"
  },
  "permissions": [
    "VIEW_PLAN_DETAILS",
    "VIEW_CONTRIBUTIONS_HISTORY",
    "REQUEST_LOAN",
    "DOWNLOAD_PLAN_STATEMENT",
    "UPDATE_BENEFICIARIES"
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
  "message": "Relacionamento não encontrado na sessão",
  "path": "/sessions/relationship"
}
```

### Códigos de Erro:
- **400**: RelationshipId inválido, relacionamento não encontrado na sessão
- **401**: AccessToken inválido, expirado ou malformado
- **404**: Sessão não encontrada no Redis
- **429**: Rate limit excedido
- **500**: Erro interno (integração, Redis)
- **503**: Serviços externos indisponíveis (FidcPermission)

## 🛡️ Política de Rate Limiting:
- **Por IP**: 30 req/min, 150 req/hora
- **Por User-Agent**: 50 req/min, 250 req/hora
- **Burst**: Até 10 req consecutivas

## 📋 Regras de Negócio:

### 1. Validações de Entrada
* **Rate limiting:** Verificar limites por IP e User-Agent
* **Se limite excedido:** Retornar erro 429 "Rate limit excedido"
* **Header Authorization:** Verificar presença do Bearer token
* **Se header ausente:** Retornar erro 401 "Token de acesso obrigatório"
* **Request Body:** Validar presença e formato do relationshipId
* **Se relationshipId inválido:** Retornar erro 400 "RelationshipId é obrigatório"

### 2. Validação do AccessToken
* **Extrair AccessToken:** Do header Authorization (Bearer {token})
* **Decodificar JWT:** Extrair claims sem validar assinatura ainda
* **Se JWT malformado:** Retornar erro 401 "Token de acesso inválido"
* **Extrair sessionId:** Da claim "sessionId" do JWT
* **Se sessionId ausente:** Retornar erro 401 "Token de acesso inválido"

### 3. Buscar Sessão e Validar Assinatura
* **Buscar sessão no Redis:** Chave `session:{sessionId}`
* **Se sessão não encontrada:** Retornar erro 404 "Sessão não encontrada ou expirada"
* **Extrair sessionSecret:** Da sessão encontrada no Redis
* **Validar assinatura JWT:** Usando sessionSecret específico da sessão
* **Se assinatura inválida:** Retornar erro 401 "Token de acesso inválido"
* **Verificar expiração:** Comparar claim "exp" com timestamp atual
* **Se token expirado:** Retornar erro 401 "Token de acesso expirado"

### 4. Validação do Relacionamento
* **Buscar relacionamento:** Procurar relationshipId na relationshipList da sessão
* **Se relacionamento não encontrado:** Retornar erro 400 "Relacionamento não encontrado na sessão"
* **Verificar status:** Relacionamento deve ter status "ACTIVE" (se aplicável)
* **Se status inativo:** Retornar erro 400 "Relacionamento inativo"

### 5. Busca de Permissões Específicas (FidcPermission)
* **Extrair dados da sessão:** partner e cpf do userInfo
* **Chamar FidcPermission:** GET /permissions com headers partner, cpf e relationshipId
* **Se erro na integração:** Retornar erro 503 "Serviço temporariamente indisponível"
* **Se sem permissões:** Continuar com array vazio (relacionamento pode não ter permissões)
* **Se sucesso:** Extrair permissões específicas do relacionamento

### 6. Atualização da Sessão
* **Definir relationshipSelected:** Copiar objeto completo do relacionamento da relationshipList
* **Substituir permissions:** Trocar permissões gerais pelas específicas do relacionamento
* **Atualizar updatedAt:** Timestamp atual da modificação
* **Salvar no Redis:** Atualizar sessão com novos dados
* **Manter TTL:** Preservar tempo de expiração original da sessão
* **Se erro ao salvar:** Retornar erro 500 "Erro interno do servidor"

### 7. Geração de Novo AccessToken
* **Manter sessionSecret:** Usar o mesmo sessionSecret da sessão (não gera novo)
* **Assinar JWT:** Com sessionSecret existente da sessão
* **Claims:**
  ```json
  {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "exp": 1692360000
  }
  ```
* **Algoritmo:** HMAC-SHA256
* **TTL:** Mesma expiração da sessão Redis

### 8. Resposta Final
* **Retornar sessão completa:** userInfo, fund, relationshipList, relationshipSelected, permissions, accessToken
* **relationshipSelected:** Objeto completo do relacionamento selecionado
* **permissions:** Permissões específicas do relacionamento (substituem as gerais)
* **Log INFO:** Relacionamento selecionado com sucesso

## 🔧 Integrações e Configurações:

### FidcPermission API
- **Base URL**: http://localhost:8082 (dev), http://localhost:8082 (uat), http://localhost:8082 (prod)
- **Endpoint**: `GET /permissions`
- **Headers**: `partner`, `cpf`, `relationshipId`
- **Request Example**: Headers com partner="prevcom", cpf="12345678901", relationshipId="REL001"
- **Response**:
  ```json
  {
    "permissions": [
      "VIEW_PLAN_DETAILS",
      "VIEW_CONTRIBUTIONS_HISTORY", 
      "REQUEST_LOAN",
      "DOWNLOAD_PLAN_STATEMENT",
      "UPDATE_BENEFICIARIES"
    ]
  }
  ```

### Redis Session Update
- **Chave**: `session:{sessionId}` (mesma chave da sessão existente)
- **TTL**: Preservar TTL original (não reset)
- **Campos atualizados**:
  ```json
  {
    "updatedAt": "2025-07-01T14:20:15",
    "relationshipSelected": {
      "id": "REL001",
      "type": "PLANO_PREVIDENCIA", 
      "name": "Plano Previdência Básico",
      "status": "ACTIVE",
      "contractNumber": "378192372163682"
    },
    "permissions": [
      "VIEW_PLAN_DETAILS",
      "VIEW_CONTRIBUTIONS_HISTORY",
      "REQUEST_LOAN",
      "DOWNLOAD_PLAN_STATEMENT", 
      "UPDATE_BENEFICIARIES"
    ]
  }
  ```

### Política de Integração:
- **Timeout**: 10 segundos para FidcPermission
- **Retry**: 3 tentativas com backoff exponencial
- **Circuit Breaker**: Mesmo padrão do FLUXO 1

### Configurações:
```kotlin
@ConfigurationProperties("fidc.auth")
data class FidcAuthConfig(
    val fidcPermission: ApiConfig = ApiConfig(),
    val session: SessionConfig = SessionConfig()
) {
    data class ApiConfig(
        val baseUrl: String = "http://localhost:8082",
        val timeoutSeconds: Int = 10,
        val retryAttempts: Int = 3
    )
    
    data class SessionConfig(
        val preserveTtl: Boolean = true,
        val updateTimestampOnSelect: Boolean = true
    )
}
```

## 📊 Observabilidade:
- **Logs INFO**: Relacionamento selecionado com sucesso, permissões específicas obtidas
- **Logs WARN**: Relacionamento sem permissões específicas, fallback para array vazio
- **Logs ERROR**: Integração FidcPermission falhou, erro ao atualizar sessão no Redis
- **Logs DEBUG**:
    - AccessToken validado com sucesso (sem dados sensíveis)
    - Relacionamento encontrado na sessionList
    - Permissões específicas retornadas pela integração
    - Estado da sessão antes e depois da atualização
- **Métricas**:
    - Contador de seleções de relacionamento por partner
    - Latência da integração FidcPermission
    - Taxa de relacionamentos selecionados vs total de relacionamentos disponíveis
    - Contador de permissões específicas vs gerais por relacionamento