# Regras de Sessão por Partner

## 📋 Visão Geral
O sistema de autenticação implementa um modelo de sessões isoladas por combinação **CPF + Partner**, garantindo que usuários possam ter múltiplas sessões ativas simultaneamente, desde que sejam para partners diferentes.

## 🏗️ Arquitetura de Sessões

### Modelo de Isolamento
```
CPF: 12345678901
├── Partner: PREVCOM → Sessão A (ativa)
├── Partner: CAIO    → Sessão B (ativa)
└── Partner: ITAU    → Sessão C (ativa)
```

### Chave Única de Sessão
- **Regra**: Uma sessão ativa por combinação CPF + Partner
- **Implementação**: 
  - Redis: Índice `cpf_index:CPF:partner` → sessionId
  - PostgreSQL: Constraint única em `(cpf, partner)`

## 🔒 Regras de Controle de Sessão

### 1. Criação de Nova Sessão
**Comportamento:**
- **Mesmo CPF + Mesmo Partner**: Invalida sessão anterior, cria nova
- **Mesmo CPF + Partner Diferente**: Mantém sessões existentes, cria nova para o partner

**Exemplo:**
```kotlin
// Situação inicial: João tem sessão ativa com PREVCOM
createSession(cpf="12345678901", partner="PREVCOM") 
// → Invalida sessão PREVCOM anterior, cria nova

createSession(cpf="12345678901", partner="CAIO")
// → Mantém sessão PREVCOM, cria nova sessão CAIO
```

### 2. Seleção de Relacionamento
**Validação de Segurança:**
- Header `partner` deve coincidir com partner da sessão
- Token deve ter sido gerado para aquela sessão específica
- Caso contrário: **403 Forbidden**

**Exemplo:**
```http
PATCH /sessions/relationship
Authorization: Bearer eyJ... (token da sessão PREVCOM)
partner: CAIO  
relationshipId: REL123
```
**Resultado:** 403 - Partner não autorizado para esta sessão

### 3. Encerramento de Sessão
**Busca Otimizada:**
1. **Preferencial**: Busca por CPF (extraído do token) + Partner (header)
2. **Fallback**: Busca por SessionId + Validação de Partner

**Validação:**
- Partner do header deve coincidir com partner da sessão
- Caso contrário: **403 Forbidden**

## 🗄️ Estrutura de Dados

### Redis
```
# Chaves de Sessão (uma por sessão ativa)
session:prevcom:uuid-123 → {...dados da sessão...}
session:caio:uuid-456    → {...dados da sessão...}

# Índices de CPF+Partner (para busca otimizada)
cpf_index:12345678901:prevcom → uuid-123
cpf_index:12345678901:caio    → uuid-456
```

### PostgreSQL
```sql
-- Tabela: tb_user_session_control
-- Constraint: UNIQUE(cpf, partner)
INSERT INTO tb_user_session_control (cpf, partner, current_session_id, is_active) 
VALUES 
  ('12345678901', 'prevcom', 'uuid-123', true),  -- ✅ Permitido
  ('12345678901', 'caio', 'uuid-456', true),     -- ✅ Permitido  
  ('12345678901', 'prevcom', 'uuid-789', true);  -- ❌ Violação de constraint
```

## 🛡️ Cenários de Segurança

### Cenário 1: Tentativa de Cross-Partner Access
```
Usuário A: CPF 111, Partner PREVCOM, SessionId abc123
Tenta usar: sessionId abc123 com header partner=CAIO
Resultado: 403 - Partner não autorizado
```

### Cenário 2: Token Hijacking
```
Atacante obtém token da sessão PREVCOM do usuário
Tenta usar com partner=CAIO para acessar dados do CAIO
Resultado: 403 - Token não pertence a sessão do CAIO
```

### Cenário 3: Session Isolation
```
Usuário logado em PREVCOM e CAIO simultaneamente
Ações em uma sessão não afetam a outra
Logout do PREVCOM não afeta sessão do CAIO
```

## ⚡ Otimizações de Performance

### Busca por CPF+Partner
**Vantagem:** Acesso direto sem varredura
```kotlin
// Otimizado - O(1)
session = sessionRepository.findByCpfAndPartner(cpf, partner)

// Não otimizado - O(1) mas sem validação precoce
session = sessionRepository.findBySessionId(sessionId)
// Depois: validar se session.partner == headerPartner
```

### Invalidação de Sessão Anterior
**Algoritmo:**
```kotlin
fun createSession(cpf: String, partner: String) {
    // 1. Buscar sessão anterior do MESMO partner
    existingSession = findByCpfAndPartner(cpf, partner)
    
    // 2. Invalidar apenas se encontrar
    existingSession?.let { invalidateSession(it) }
    
    // 3. Criar nova sessão
    newSession = createNewSession(cpf, partner, ...)
}
```

## 🔍 Casos de Uso Práticos

### Portal Multi-Partner
```
Usuário acessa portal que suporta múltiplos partners
1. Login PREVCOM → Sessão A ativa
2. Troca para CAIO → Sessão B ativa (Sessão A permanece)
3. Volta para PREVCOM → Usa Sessão A existente
4. Logout PREVCOM → Sessão B permanece ativa
```

### Aplicação Mobile
```
App mobile permite alternância rápida entre partners
- Sessões ficam ativas em background
- Mudança de partner = mudança de contexto de sessão
- Performance otimizada (sem re-login constante)
```

## 🚨 Limitações e Considerações

### Memória Redis
- Múltiplas sessões por usuário aumentam uso de memória
- TTL individual por sessão mitiga acúmulo
- Monitoramento de sessões ativas por usuário pode ser necessário

### Complexidade de Gerenciamento
- Admin tools devem considerar múltiplas sessões por CPF
- Auditoria deve rastrear por combinação CPF+Partner
- Métricas devem segmentar por partner

### Casos Extremos
```
# Usuário com muitas sessões
CPF: 12345678901
├── Partner: PREVCOM → Sessão 1
├── Partner: CAIO    → Sessão 2  
├── Partner: ITAU    → Sessão 3
├── Partner: BB      → Sessão 4
└── Partner: CEF     → Sessão 5
```

**Recomendação:** Implementar limite máximo de partners ativos por CPF se necessário.

## 📊 Monitoramento e Alertas

### Métricas Essenciais
- **Gauge**: Sessões ativas por partner
- **Histogram**: Distribuição de sessões por CPF (detectar anomalias)
- **Counter**: Tentativas de cross-partner access (segurança)
- **Timer**: Latência de operações por CPF+Partner vs SessionId

### Alertas Recomendados
- CPF com mais de X partners ativos simultaneamente
- Taxa alta de erros 403 (possível ataque)
- Crescimento anômalo de sessões por partner