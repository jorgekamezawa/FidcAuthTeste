# FLUXO: Obter JWT Secret

## 📋 Visão Geral
- **Trigger**: Chamada API REST pelo portal
- **Objetivo**: Disponibilizar o hash da secret JWT para assinatura de tokens no início de sessão
- **Microserviço**: `fidc-auth`
- **Endpoint**: `GET /v1/sessions/jwt-secret`

## 🔄 Contrato da API

### Headers Obrigatórios:
Nenhum header obrigatório

### Headers Opcionais:
- `x-correlation-id` (gerado automaticamente pelo CorrelationIdFilter se ausente)

### Request:
```http
GET /v1/sessions/jwt-secret
```

### Response (Sucesso):
```json
{
  "secret": "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456"
}
```

### Response (Erro):
```json
{
  "timestamp": "2025-08-28T14:45:32",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Erro interno do servidor",
  "path": "/v1/sessions/jwt-secret"
}
```

### Códigos de Erro:
- **500**: Erro interno (falha ao buscar secret no AWS Secrets Manager)
- **503**: AWS Secrets Manager indisponível

## 🛡️ Política de Segurança:
- **Rate Limiting**: Não aplicado (fluxo de configuração inicial)
- **Autenticação**: Não exigida (endpoint público para configuração)
- **Partner**: Não obrigatório (secret é global)

## 📋 Regras de Negócio:

### 1. Processamento Direto
* **Sem validações de entrada:** Não há headers obrigatórios ou parâmetros

### 2. Busca da JWT Secret no AWS Secrets Manager
* **Buscar secret:** Obter secret JWT do AWS Secrets Manager usando AwsSecretManagerService
* **Nome da secret:** Utilizar configuração `aws.secret-manager.jwt.name` do application.properties
* **Se erro na busca:** Retornar erro 503 "Serviço temporariamente indisponível"
* **Se secret não encontrada:** Retornar erro 500 "Erro interno do servidor"

### 3. Processamento da Secret
* **Extrair signingKey:** Obter campo `signingKey` do JSON da secret
* **Se campo ausente:** Retornar erro 500 "Erro interno do servidor"
* **Validar formato:** Verificar se signingKey não está vazio
* **Se formato inválido:** Retornar erro 500 "Erro interno do servidor"

### 4. Resposta Final
* **Retornar hash:** Resposta contendo apenas o campo `secret` com o valor da signingKey
* **Log:** Registrar sucesso na obtenção da JWT secret (sem expor o valor)

## 🔧 Integrações Externas:

### AWS Secrets Manager
- **Service**: AwsSecretManagerService (já existente no projeto)
- **Método**: `getSecretAsMap(secretName: String): Map<String, Any>?`
- **Secret Name**: Configurado via `aws.secret-manager.jwt.name` (ex: "dev/jwt/secret")
- **Estrutura da Secret**:
  ```json
  {
    "signingKey": "dGhpc2lzYXNlY3JldGtleWZvcmp3dHNpZ25pbmdmaWRjYXV0aGRldg=="
  }
  ```
- **Tratamento de Erro**: Se indisponível → Retornar erro 503 "Serviço temporariamente indisponível"
- **Secret não encontrada**: Retornar erro 500 "Erro interno do servidor"

## 📊 Observabilidade e Logs:
- **Logs INFO**: 
  - Início do processo de obtenção da JWT secret
  - Sucesso na obtenção da JWT secret
- **Logs WARN**: 
  - Nenhum caso específico (endpoint sem validações)
- **Logs ERROR**: 
  - Falhas na integração com AWS Secrets Manager
  - Secret JWT não encontrada ou formato inválido
- **Logs DEBUG**: 
  - Detalhes do processo de busca da secret (sem expor valores)

**Correlation ID**: Automaticamente incluído em todos os logs pelo filtro do sistema.

## 🔒 Considerações de Segurança:
- **Não loggar valores**: Secret JWT nunca deve aparecer em logs
- **Endpoint público**: Não requer autenticação por ser usado para configuração inicial
- **Rate limiting**: Não aplicado pois é um endpoint de configuração
- **HTTPS obrigatório**: Em produção, deve ser usado apenas via HTTPS

## ⚙️ Configurações do Sistema:
- **JWT Secret Name**: Configurado via `aws.secret-manager.jwt.name` no application.properties
- **Timeout AWS**: Utiliza configurações padrão do AwsSecretManagerService
- **Logs**: Nível INFO para operações normais, ERROR para falhas

## 🎯 Casos de Uso:
1. **Portal Web**: Obter secret para assinar tokens JWT antes de iniciar sessões
2. **Mobile App**: Obter secret para validação de tokens localmente
3. **Microsserviços**: Sincronizar secret JWT entre diferentes serviços