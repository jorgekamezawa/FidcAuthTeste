# FLUXO: Encerrar Sessão

## 📋 Visão Geral
- **Trigger**: Chamada API REST pelo portal (logout manual)
- **Objetivo**: Encerrar sessão ativa, invalidar AccessToken e limpar estado da sessão
- **Microserviço**: `fidc-auth`
- **Endpoint**: `DELETE /v1/sessions`

## 📄 Contrato da API

### Headers Obrigatórios:
- `Authorization` (Bearer {accessToken})
- `partner` (prevcom, caio, etc.)

### Headers Opcionais:
- `x-correlation-id` (gerado automaticamente pelo CorrelationIdFilter se ausente)


### Request Body:
```json
{}
```
*Body vazio - operação baseada apenas no AccessToken*

### Response (Sucesso):
```
Status: 204 No Content
Body: (vazio)
```

### Response (Erro):
```json
{
  "timestamp": "2025-08-18T14:45:32",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token de acesso inválido",
  "path": "/sessions"
}
```

### Códigos de Erro:
- **400**: Header partner ausente, token malformado
- **401**: Token com assinatura inválida
- **403**: Partner do request diferente do partner da sessão
- **404**: Sessão não encontrada (retorna 204 - operação idempotente)
- **500**: Erro interno (PostgreSQL indisponível)
- **503**: Redis indisponível (falha ao verificar/remover sessão)


## 📋 Regras de Negócio:

### 1. Validações Simples de Entrada
* **Headers obrigatórios:** Validar presença de Authorization, partner
* **Se headers ausentes:** Retornar erro 400 "Headers obrigatórios ausentes"

### 2. Extração de SessionId e Busca da Sessão
* **Extrair sessionId:** Do AccessToken no header Authorization
* **Se token malformado:** Retornar erro 400 "Token de acesso contém sessionId inválido"
* **Buscar sessão:** Localizar sessão ativa no cache usando sessionId
* **Se sessão não encontrada no Redis:** Verificar estado no PostgreSQL
  * **Buscar por sessionId no banco:** Localizar registro na tabela de controle de sessão
  * **Se encontrada no banco:**
    * **Verificar partner:** Partner do header deve coincidir com o da sessão
    * **Se partner não autorizado:** Retornar erro 403 "Partner não autorizado para esta sessão"
    * **Se já inativa:** Retornar 204 No Content (operação idempotente)
    * **Se ainda ativa:** Marcar como inativa e retornar 204 No Content
  * **Se não encontrada:** Retornar 204 No Content (operação idempotente)

### 3. Validação de Partner e AccessToken
* **Validar partner:** Verificar se partner do header coincide com partner da sessão
* **Se partner não autorizado:** Retornar erro 403 "Partner não autorizado para esta sessão"
* **Validar AccessToken:** Verificar assinatura JWT usando sessionSecret da sessão (com tratamento seguro de tokens expirados)
* **Se token inválido:** Continuar com encerramento (comportamento seguro para tokens expirados)

### 4. Remoção Atômica da Sessão
* **Remover do cache:** Deletar sessão do Redis
* **Se erro no Redis:** Retornar erro 500 "Serviço temporariamente indisponível"
* **Atualizar controle de sessão:** Marcar sessão como inativa no banco de dados
* **Se erro no banco:** Retornar erro 500 "Erro interno do servidor"
* **Tratamento de inconsistências:** Continuar mesmo se sessão não for encontrada no banco

### 5. Resposta Final
* **Retornar 204:** Sem conteúdo (sessão encerrada com sucesso)
* **Log:** Registrar encerramento manual da sessão

## 🔧 Configurações do Sistema:

### Operações de Persistência
- **Cache (Redis)**: Remover sessão completa do cache
- **Banco de Dados**: Marcar sessão como inativa na tabela de controle
- **Tratamento de Erro**: Operação transacional com rollback em caso de falha

### Configurações de Timeout
- **Operação Idempotente**: Retorna 204 mesmo se sessão já não existir
- **Validação Segura**: Continua encerramento mesmo com tokens expirados

## 📊 Observabilidade e Logs:

- **Logs INFO**: 
  - Início do processo de encerramento de sessão
  - Sessão não encontrada no Redis (verificação PostgreSQL)
  - Sessão desativada no PostgreSQL
  - Sessão encerrada manualmente com sucesso
- **Logs WARN**: 
  - Erros de negócio e validação
  - Inconsistências detectadas entre cache e banco
  - Dados corrompidos encontrados no Redis
- **Logs ERROR**: 
  - Falhas em operações de persistência (cache, banco)
  - Erros inesperados no processamento
- **Logs DEBUG**: 
  - Sessão já estava inativa no PostgreSQL
  - Sessão não encontrada nem no Redis nem no PostgreSQL
  - Confirmações de operações de remoção
  - Estados intermediários do fluxo de verificação

**Correlation ID**: Automaticamente incluído em todos os logs pelo filtro do sistema.



## ⚙️ Casos Especiais:

### Operação Idempotente
- **Sessão Não Encontrada no Redis**: Verifica PostgreSQL e ajusta estado se necessário
- **Sessão Já Inativa no PostgreSQL**: Retorna 204 (objetivo já alcançado)
- **Tokens Expirados**: Continua com encerramento da sessão (comportamento seguro)
- **Sessão Inexistente**: Retorna 204 (operação idempotente)

### Segurança
- **Partner Mismatch**: Retorna 403 sem fazer alterações (impede encerramento cruzado)
- **Validação de Token**: Usando sessionSecret específico da sessão

### Tratamento de Falhas
- **Falhas de Persistência**: Retorna erro 500 para garantir que o cliente saiba que a operação falhou
- **Inconsistências**: Registradas em logs para monitoramento e análise