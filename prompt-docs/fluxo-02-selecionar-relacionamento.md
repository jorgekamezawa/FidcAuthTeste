# FLUXO: Selecionar Relacionamento

## 📋 Visão Geral
- **Trigger**: Chamada API REST pelo portal
- **Objetivo**: Selecionar relacionamento específico, buscar permissões contextuais e atualizar sessão
- **Microserviço**: `fidc-auth`
- **Endpoint**: `PATCH /v1/sessions/relationship`

## 📄 Contrato da API

### Headers Obrigatórios:
- `authorization` (Bearer {accessToken})
- `partner` (Identificador do partner - deve coincidir com o partner da sessão)
- `relationshipId` (ID do relacionamento a ser selecionado)
- `user-agent` (para rate limiting)

### Headers Opcionais:
- `x-correlation-id` (gerado automaticamente pelo CorrelationIdFilter se ausente)

### Headers Automáticos (para rate limiting):
- `x-forwarded-for` ou `remote-addr` (IP do cliente)

### Request Body:
```json
{}
```
*Body vazio - relationshipId vem via header*

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
  "message": "Header relationshipId é obrigatório",
  "path": "/sessions/relationship"
}
```

### Códigos de Erro:
- **400**: Header partner/relationshipId ausente, relacionamento não encontrado na sessão
- **401**: AccessToken inválido, expirado ou malformado
- **403**: Partner não autorizado para esta sessão
- **404**: Sessão não encontrada no Redis
- **429**: Rate limit excedido
- **500**: Erro interno (integração, Redis)
- **503**: Serviços externos indisponíveis (FidcPermission)

## 🛡️ Política de Rate Limiting:
- **Por IP**: 30 req/min, 150 req/hora
- **Por User-Agent**: 50 req/min, 250 req/hora
- **Burst**: Até 10 req consecutivas

## 📋 Regras de Negócio:

### 1. Validações Simples de Entrada
* **Headers obrigatórios:** Validar presença de partner, authorization, relationshipId, user-agent
* **Se headers ausentes:** Retornar erro 400 "Headers obrigatórios ausentes"
* **Rate limiting:** Verificar limites por IP e User-Agent
* **Se limite excedido:** Retornar erro 429 "Rate limit excedido"

### 2. Autenticação e Validação de Sessão
* **Extrair sessionId:** Do AccessToken no header Authorization
* **Se token malformado:** Retornar erro 400 "Token de acesso contém sessionId inválido"
* **Buscar sessão:** Localizar sessão ativa no cache usando sessionId
* **Se sessão não encontrada:** Retornar erro 404 "Sessão não encontrada ou expirada"
* **Validar partner:** Verificar se partner do header coincide com partner da sessão
* **Se partner não autorizado:** Retornar erro 403 "Partner não autorizado para esta sessão"
* **Validar AccessToken:** Verificar assinatura JWT usando sessionSecret da sessão
* **Se token inválido:** Retornar erro 401 "Token de acesso inválido"

### 3. Validação do Relacionamento
* **Buscar relacionamento:** Procurar relationshipId na lista de relacionamentos da sessão
* **Se relacionamento não encontrado:** Retornar erro 400 "Relacionamento não encontrado na sessão"
* **Verificar status:** Relacionamento deve ter status "ACTIVE"
* **Se status inativo:** Retornar erro 400 "Relacionamento inativo"

### 4. Busca de Permissões Específicas
* **Extrair dados da sessão:** partner e cpf do userInfo
* **Buscar permissões:** Obter permissões específicas do relacionamento selecionado
* **Se erro na integração:** Retornar erro 503 "Serviço temporariamente indisponível"
* **Se sem permissões:** Retornar erro 400 "Nenhuma permissão encontrada para o relacionamento selecionado"
* **Se sucesso:** Obter lista de permissões contextuais do relacionamento

### 5. Atualização da Sessão
* **Selecionar relacionamento:** Definir relacionamento escolhido na sessão
* **Atualizar permissões:** Substituir permissões gerais pelas específicas do relacionamento
* **Persistir sessão:** Salvar sessão atualizada no cache
* **Se erro ao salvar:** Retornar erro 500 "Erro interno do servidor"

### 6. Reutilização do AccessToken
* **Manter AccessToken original:** Usar o mesmo AccessToken recebido na requisição
* **Não gerar novo token:** Token atual já foi validado e ainda é válido
* **Preservar expiração:** Manter a mesma expiração do token original

### 7. Resposta Final
* **Preparar resposta:** Organizar dados da sessão atualizada com relacionamento selecionado
* **Retornar dados completos:** userInfo, fund, relationshipList, relationshipSelected, permissions, accessToken
* **permissions:** Permissões contextuais do relacionamento selecionado
* **Log:** Registrar sucesso na seleção do relacionamento

## 🔧 Integrações Externas:

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
- **Chave**: `fidc:session:{partner}:{sessionId}` (mesma chave da sessão existente)
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

### Configurações do Sistema:
- **TTL da Sessão**: Preservar TTL original da sessão (não resetar)
- **Timeout Integrações**: 10 segundos com retry automático
- **Rate Limiting**: Limites por IP e User-Agent conforme política definida
- **Cache**: Atualizar sessão existente sem alterar TTL

## 📊 Observabilidade e Logs:
- **Logs INFO**: 
  - Início do processo de seleção de relacionamento
  - Sucesso na seleção com sessionId e relationshipId
- **Logs WARN**: 
  - Erros de negócio e validação
  - Dados corrompidos encontrados no Redis
- **Logs ERROR**: 
  - Falhas em integrações externas
  - Erros de persistência (cache)
  - Erros inesperados no processamento
- **Logs DEBUG**: 
  - Detalhes da validação de sessão e relacionamento
  - Confirmações de operações de persistência
  - Estados intermediários do fluxo

**Correlation ID**: Automaticamente incluído em todos os logs pelo filtro do sistema.