# Mocks Alinhados - UserManagement e FidcPermission

## 📋 Visão Geral
Os mocks dos serviços `UserManagementService` e `FidcPermissionService` foram alinhados para simular comportamentos realistas e consistentes entre si, suportando o sistema de sessões isoladas por CPF + Partner.

## 🔧 Regras Implementadas

### Regras Comuns (UserManagement + FidcPermission)
1. **Partners suportados**: `"prevcom"` e `"caio"` (case-insensitive)
2. **CPF terminando em 4 ou 5**: Simula erro/sem dados
3. **Outros partners**: Erro/sem permissões
4. **Case-insensitive**: Partners aceitos em qualquer case

## 🏢 UserManagementService

### Estrutura de Dados

#### PREVCOM (Previdência)
```kotlin
Partner: "prevcom"
User: João Silva Santos (joao.silva@email.com)
Fund: PREVCOM001 - Prevcom Previdência RS
Relationships:
- REL001: Plano Previdência Básico (ACTIVE)
- REL002: Plano Previdência Premium (ACTIVE)
```

#### CAIO (Investimentos)
```kotlin
Partner: "caio" 
User: Maria Oliveira Costa (maria.costa@empresa.com)
Fund: CAIO001 - CAIO Fundo de Investimento
Relationships:
- REL003: Conta Investimentos Master (ACTIVE)
```

### Cenários de Teste
| CPF Final | Partner | Resultado |
|-----------|---------|-----------|
| 1,2,3,6,7,8,9,0 | `prevcom` | ✅ Dados PREVCOM |
| 1,2,3,6,7,8,9,0 | `caio` | ✅ Dados CAIO |
| **4 ou 5** | Qualquer | ❌ UserManagementException |
| Qualquer | Outros | ❌ UserManagementException |

## 🔐 FidcPermissionService

### Sistema de Permissões Escalonadas

#### 1. Sem RelationshipId (Permissões Gerais)
```kotlin
// PREVCOM (3 permissões básicas)
- VIEW_PROFILE
- VIEW_PLAN_SUMMARY
- DOWNLOAD_BASIC_DOCUMENTS

// CAIO (3 permissões básicas)
- VIEW_PROFILE  
- VIEW_PORTFOLIO_SUMMARY
- DOWNLOAD_BASIC_REPORTS
```

#### 2. Com RelationshipId (Permissões Específicas)

**PREVCOM - REL001 (Plano Básico):**
```kotlin
// 6 permissões (básicas + intermediárias)
- VIEW_PROFILE
- VIEW_PLAN_SUMMARY
- DOWNLOAD_BASIC_DOCUMENTS
- VIEW_PLAN_DETAILS
- VIEW_CONTRIBUTIONS_HISTORY
- DOWNLOAD_PLAN_STATEMENT
```

**PREVCOM - REL002 (Plano Premium):**
```kotlin
// 11 permissões (básicas + intermediárias + avançadas)
- VIEW_PROFILE
- VIEW_PLAN_SUMMARY
- DOWNLOAD_BASIC_DOCUMENTS
- VIEW_PLAN_DETAILS
- VIEW_CONTRIBUTIONS_HISTORY
- DOWNLOAD_PLAN_STATEMENT
- UPDATE_BENEFICIARIES
- REQUEST_LOAN
- MANAGE_PLAN_CONTRIBUTIONS
- REQUEST_PLAN_CHANGES
- VIEW_INVESTMENT_OPTIONS
```

**CAIO - REL003 (Conta Master):**
```kotlin
// 12 permissões (conjunto completo de investimentos)
- VIEW_PROFILE
- VIEW_PORTFOLIO_SUMMARY
- DOWNLOAD_BASIC_REPORTS
- VIEW_INVESTMENT_DETAILS
- VIEW_TRANSACTION_HISTORY
- DOWNLOAD_DETAILED_REPORTS
- CREATE_INVESTMENT_SIMULATION
- EXECUTE_BUY_ORDERS
- EXECUTE_SELL_ORDERS
- TRANSFER_BETWEEN_FUNDS
- MANAGE_AUTOMATIC_INVESTMENTS
- REQUEST_INVESTMENT_ADVICE
```

#### 3. RelationshipId Inválido
```kotlin
// Permissões mínimas (2 permissões)
- VIEW_PROFILE
- VIEW_PLAN_SUMMARY (PREVCOM)
- VIEW_PORTFOLIO_SUMMARY (CAIO)
```

### Cenários de Teste FidcPermission
| CPF Final | Partner | RelationshipId | Resultado |
|-----------|---------|---------------|-----------|
| 1,2,3,6,7,8,9,0 | `prevcom` | `null` | ✅ 3 permissões gerais |
| 1,2,3,6,7,8,9,0 | `prevcom` | `REL001` | ✅ 6 permissões básico |
| 1,2,3,6,7,8,9,0 | `prevcom` | `REL002` | ✅ 11 permissões premium |
| 1,2,3,6,7,8,9,0 | `caio` | `null` | ✅ 3 permissões gerais |
| 1,2,3,6,7,8,9,0 | `caio` | `REL003` | ✅ 12 permissões master |
| **4 ou 5** | Qualquer | Qualquer | ❌ Lista vazia |
| Qualquer | Outros | Qualquer | ❌ Lista vazia |
| Qualquer | Válido | `REL999` | ✅ 2 permissões mínimas |

## 🔄 Fluxo de Integração

### Fluxo 1: Criação de Sessão
```
1. UserManagement.getUser(cpf, partner)
   → Retorna relationships: [REL001, REL002] ou [REL003]

2. FidcPermission.getPermissions(cpf, partner, relationshipId=null)
   → Retorna 3 permissões gerais
   
3. Sessão criada com relationships + permissões gerais
```

### Fluxo 2: Seleção de Relationship
```
1. SelectRelationship(relationshipId=REL002)
2. FidcPermission.getPermissions(cpf, partner, relationshipId=REL002)
   → Retorna 11 permissões específicas (Premium)
   
3. Sessão atualizada com permissões específicas
```

## 📊 Matriz de Permissões

| Cenário | PREVCOM Geral | PREVCOM REL001 | PREVCOM REL002 | CAIO Geral | CAIO REL003 |
|---------|---------------|----------------|----------------|------------|-------------|
| **Permissões** | 3 | 6 | 11 | 3 | 12 |
| **Nível** | Básico | Intermediário | Avançado | Básico | Completo |
| **Funcionalidades** | Visualização | Visualização + Detalhes | Tudo + Gestão | Visualização | Transacional |

## 🚨 Casos Extremos Suportados

### 1. Partner Case Variations
```
"PREVCOM", "prevcom", "Prevcom", "PrevCom" → Todos funcionam
"CAIO", "caio", "Caio", "CaIo" → Todos funcionam
```

### 2. CPF Edge Cases
```
"12345678904" + "prevcom" → UserManagement lança exceção
"12345678905" + "caio" → FidcPermission retorna lista vazia
```

### 3. RelationshipId Cross-Partner
```
REL001 (PREVCOM) com partner CAIO → 2 permissões mínimas
REL003 (CAIO) com partner PREVCOM → 2 permissões mínimas
```

### 4. Partners Inexistentes
```
"itau", "bb", "nubank" → UserManagement lança exceção
"itau", "bb", "nubank" → FidcPermission retorna lista vazia
```

## 🎯 Benefícios da Implementação

### 1. **Consistência Total**
- Mesmas regras de CPF e Partner entre serviços
- Dados alinhados (RelationshipIds correspondem)
- Comportamentos previsíveis

### 2. **Simulação Realística**
- Escalabilidade de permissões (3 → 6 → 11 → 12)
- Diferenciação por tipo de produto (Previdência vs Investimentos)
- Casos de erro bem definidos

### 3. **Testabilidade Completa**
- CPFs específicos para testar erros
- Partners para testar sucesso/erro
- RelationshipIds para testar escalabilidade

### 4. **Facilita Desenvolvimento**
- Previsibilidade nos mocks facilita testes
- Documentação clara dos comportamentos
- Suporte a múltiplos cenários de teste