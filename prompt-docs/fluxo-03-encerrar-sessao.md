# FLUXO: Encerrar Sessão

## 📋 Visão Geral
- **Trigger**: Chamada API REST pelo portal (logout manual)
- **Objetivo**: Encerrar sessão ativa, invalidar AccessToken e limpar estado da sessão
- **Microserviço**: `fidc-auth`
- **Endpoint**: `DELETE /sessions`

## 📄 Contrato da API

### Headers Obrigatórios:
- `authorization` (Bearer {accessToken} do FLUXO 1/2)
- `partner` (prevcom, caio, etc.)

### Headers Opcionais:
- `x-correlation-id` (gerado automaticamente se ausente)

### Headers Automáticos (para rate limiting):
- `x-forwarded-for` ou `remote-addr` (IP do cliente)
- `user-agent` (identificação do browser/client)

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
- **429**: Rate limit excedido
- **500**: Erro interno (PostgreSQL indisponível)
- **503**: Redis indisponível (falha ao verificar/remover sessão)

## 🛡️ Política de Rate Limiting:
- **Por IP**: 10 req/min, 50 req/hora
- **Por User-Agent**: 15 req/min, 75 req/hora
- **Burst**: Até 5 req consecutivas

## 📋 Regras de Negócio:

### 1. Validações de Entrada
* **Rate limiting:** Verificar limites por IP e User-Agent
* **Se limite excedido:** Retornar erro 429 "Rate limit excedido"
* **Header Authorization:** Verificar presença do Bearer token
* **Se header ausente:** Retornar erro 401 "Token de acesso obrigatório"
* **Header partner:** Verificar presença do partner
* **Se header ausente:** Retornar erro 400 "Header partner é obrigatório"

### 2. Validação do AccessToken
* **Extrair AccessToken:** Do header Authorization (Bearer {token})
* **Decodificar JWT:** Extrair claims sem validar assinatura ainda
* **Se JWT malformado:** Retornar erro 401 "Token de acesso inválido"
* **Extrair sessionId:** Da claim "sessionId" do JWT
* **Se sessionId ausente:** Retornar erro 401 "Token de acesso inválido"

### 3. Buscar Sessão e Validar Contexto
* **Buscar sessão no Redis:** Chave `session:{sessionId}`
* **Se sessão não encontrada:** Retornar 204 (operação idempotente - sessão já encerrada/expirada)
* **Extrair sessionSecret:** Da sessão encontrada no Redis
* **Validar assinatura JWT:** Usando sessionSecret específico da sessão
* **Se ExpiredJwtException (token expirado):** Continuar com invalidação da sessão (comportamento normal)
* **Se SignatureException (assinatura inválida):** Retornar erro 401 "Token de acesso com assinatura inválida"
* **Se MalformedJwtException (token malformado):** Retornar erro 400 "Token de acesso malformado"

### 4. Validação do Partner
* **Extrair partner da sessão:** Campo "partner" dos dados da sessão no Redis
* **Comparar partners:** Header partner vs partner da sessão
* **Se partners diferentes:** Retornar erro 403 "Partner não autorizado para esta sessão"

### 5. Remoção da Sessão (Operação Atômica)
* **Remover do Redis:** Deletar chave `session:{sessionId}`
* **Se erro no Redis:** Retornar erro 503 "Serviço temporariamente indisponível"
* **Atualizar PostgreSQL:** Buscar por current_session_id e marcar is_active = false
* **Se erro no PostgreSQL:** Retornar erro 500 "Erro interno do servidor"
* **Se sessão não encontrada no PostgreSQL:** Continuar normalmente (inconsistência será resolvida pelo job de limpeza)

### 6. Resposta Final
* **Retornar 204:** Sem conteúdo (sessão encerrada com sucesso)
* **Log INFO:** Sessão encerrada manualmente pelo usuário

## 🔧 Configurações e Infraestrutura:

### Job de Limpeza de Inconsistências
Executa periodicamente (a cada 5 minutos) para resolver inconsistências entre Redis e PostgreSQL:
- Busca sessões marcadas como ativas no PostgreSQL
- Verifica se essas sessões ainda existem no Redis
- Se a sessão não existe mais no Redis (expirou por TTL), marca como inativa no PostgreSQL
- Registra quantidade de sessões processadas e inconsistências encontradas

### PostgreSQL Operation
Operações realizadas no banco de dados:
- Buscar sessões ativas através da tabela user_session_control
- Desativar sessão específica atualizando is_active = false baseado no current_session_id

### Redis Operation
Operações realizadas no Redis:
- Verificar existência da sessão através da chave session:{sessionId}
- Remover sessão completa do cache Redis

### Política de Integração:
- **Redis Timeout**: 5 segundos
- **PostgreSQL Timeout**: 10 segundos
- **Retry**: Não aplicável para DELETE (operação única)

### Configurações:
Configurações específicas para o encerramento de sessões:
- Tempo limite para operações Redis: 5 segundos
- Tempo limite para operações PostgreSQL: 10 segundos
- Intervalo do job de limpeza: 5 minutos
- Tamanho do lote para processamento de sessões: 100 registros por vez

## 📊 Observabilidade:

### Logs Estruturados:
- **Logs INFO**:
  - Sessão encerrada manualmente com sucesso
  - Job de limpeza executado (quantidade de sessões processadas)
  - Sessões expiradas detectadas e desativadas automaticamente
- **Logs WARN**:
  - Sessão não encontrada no PostgreSQL (inconsistência detectada)
  - Partner mismatch entre request e sessão
- **Logs ERROR**:
  - Falha ao remover sessão do Redis
  - Falha ao atualizar PostgreSQL
  - Job de limpeza falhou
- **Logs DEBUG**:
  - AccessToken validado com sucesso
  - Operação de remoção iniciada
  - Estado das operações Redis e PostgreSQL

### Métricas:
- **Contador**: Sessões encerradas manualmente vs automaticamente (TTL)
- **Contador**: Inconsistências detectadas pelo job de limpeza
- **Gauge**: Sessões ativas no Redis vs PostgreSQL (diferença)
- **Timer**: Latência da operação DELETE /sessions
- **Contador**: Tentativas de encerramento com partner inválido
- **Contador**: Execuções do job de limpeza e sessões processadas



## ⚙️ Tratamento de Casos Extremos: 

### Cenário 1: Redis Indisponível
- **Comportamento**: Retorna erro 503 imediatamente
- **Justificativa**: Não pode garantir que a sessão foi invalidada
- **Recovery**: Cliente deve tentar novamente quando Redis voltar

### Cenário 2: PostgreSQL Indisponível
- **Comportamento**: Remove do Redis, depois falha no PostgreSQL → Erro 500
- **Justificativa**: Sessão fica inconsistente (inativa no Redis, ativa no PostgreSQL)
- **Recovery**: Job de limpeza resolve a inconsistência posteriormente

### Cenário 3: Sessão Já Expirada
- **Comportamento**: Retorna 204 (idempotente)
- **Justificativa**: Objetivo já foi alcançado (sessão não está ativa)

### Cenário 4: Partner Mismatch
- **Comportamento**: Retorna 403 sem fazer alterações
- **Justificativa**: Segurança - impede que um partner encerre sessões de outro