# 🏛️ DOMAIN-LAYER - CAMADA DE DOMÍNIO

---
id: domain-layer
version: 1.0.0
requires: [meta-prompt, project-context, initial-setup, infrastructure-base]
provides: [domain-entities, domain-services, repository-interfaces, business-rules]
optional: false
---

## 🎯 SEU PAPEL

Você irá implementar a camada de domínio com as regras de negócio centrais, seguindo DDD pragmático. Esta camada contém entities mutáveis com ID duplo para bancos relacionais, enums com comportamentos, interfaces de repository e domain services para lógicas complexas.

## 📋 PRÉ-REQUISITOS

Antes de iniciar:
1. **Entidades identificadas** no PROJECT-CONTEXT
2. **Regras de negócio** mapeadas nos fluxos
3. **Módulo shared** já criado com exceções base
4. **Tipo de banco** definido (relacional ou não-relacional)

## 🔄 FLUXO DE GERAÇÃO

### 1. Confirmação das Entidades e Banco
```
"Baseado no contexto, identifiquei estas entidades de domínio:

1. [Entidade A]: [descrição do que representa]
   - Responsabilidade: [o que gerencia]
   - Estados: [se tiver enum de status]
   - Persistência: [PostgreSQL/MongoDB/etc]

2. [Entidade B]: [descrição]
   - Responsabilidade: [o que gerencia]
   - Relacionamento com: [outras entidades]
   - Persistência: [tipo de banco]

IMPORTANTE: Vejo que usaremos [PostgreSQL/outro]. 
- Para bancos RELACIONAIS: entities terão id (Long) + externalId (UUID)
- Para NÃO-RELACIONAIS: apenas id (UUID)

Está correto?"
```

### 2. Distinção entre Soft Delete e Ativo/Inativo
```
"Vejo que algumas entidades podem ter diferentes necessidades de 'remoção'.
É importante distinguir dois conceitos:

1. **ATIVO/INATIVO (Status)**:
   - Registro pode ser reativado
   - É uma regra de negócio comum
   - Ex: Usuário suspenso, Produto fora de linha
   - Implementado como campo status/enum

2. **SOFT DELETE (Auditoria)**:
   - Registro NUNCA volta (sem restore)
   - Apenas para auditoria/compliance
   - Ex: Dados financeiros, registros legais
   - Implementado como deleted_at
   - Requer Domain Service para validações

Para cada entidade, o que se aplica?
- [Entidade A]: [ ] Ativo/Inativo  [ ] Soft Delete  [ ] Nenhum
- [Entidade B]: [ ] Ativo/Inativo  [ ] Soft Delete  [ ] Nenhum

IMPORTANTE: Se for apenas ativar/desativar, use status. 
Soft delete é DEFINITIVO e para casos específicos de auditoria."
```

### 3. Identificação de Enums
```
"Para as entidades, identifiquei estes enums:

- [Nome]StatusEnum: [valores possíveis]
- [Tipo]Enum: [valores]
- OrderStatusEnum: [PENDING, PROCESSING, COMPLETED, CANCELLED]

Todos os enums terão:
- Sufixo 'Enum'
- Factory methods: fromValue() e fromValueOrNull()
- Métodos básicos de comportamento
- Exceções customizadas

Confirma estes enums?"
```

### 4. Domain Services
```
"Analisando a complexidade, identifiquei necessidade de Domain Services para:

[SE CONFIRMOU SOFT DELETE]
- [Entidade]DeletionDomainService: Para validar e executar soft delete
  Justificativa: Soft delete requer validações antes da exclusão definitiva

[SE TIVER LÓGICA COMPLEXA]
- [Nome]DomainService: Para [operação complexa]
  Justificativa: [por que não cabe em factory method]

Concorda com estes services?"
```

## 📁 ESTRUTURAS A SEREM GERADAS

### 1. MÓDULO SHARED (Complementar)

#### shared/src/main/kotlin/[group/path]/shared/exception/
```kotlin
// Exceções de domínio específicas

sealed class DomainException(
    message: String,
    cause: Throwable? = null
) : BusinessException(message, cause)

// Para cada contexto de domínio
class [Contexto]ValidationException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)

class [Contexto]NotFoundException(
    message: String
) : DomainException(message)

class [Contexto]BusinessRuleException(
    message: String
) : DomainException(message)

// Exceção específica para enums
class Invalid[Contexto]EnumException(
    value: String,
    enumType: String
) : DomainException("Valor '$value' inválido para $enumType")
```

#### shared/src/main/kotlin/[group/path]/shared/dto/
```kotlin
// DTOs para queries complexas

data class [Entidade]Filter(
    val [campo1]: String? = null,
    val [campo2]: String? = null,
    val [campoData]From: LocalDate? = null,
    val [campoData]To: LocalDate? = null,
    val status: List<String>? = null,
    val includeDeleted: Boolean = false
)

// DTO para paginação
data class Page<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
```

### 2. ESTRUTURA DO DOMAIN

```
domain/
└── src/main/kotlin/[group/path]/domain/
    └── [contexto]/
        ├── entity/
        │   └── [Entidade].kt
        ├── enum/
        │   ├── [Nome]StatusEnum.kt
        │   └── [Tipo]Enum.kt
        ├── repository/
        │   └── [Entidade]Repository.kt
        └── service/
            ├── [Entidade]DeletionDomainService.kt  // APENAS se confirmou soft delete
            └── [Nome]DomainService.kt             // se tiver lógica complexa
```

### 3. ENTIDADES DE DOMÍNIO

#### Template para Banco RELACIONAL (ID Duplo)
```kotlin
package [group].domain.[contexto].entity

import [group].shared.exception.*
import [group].shared.constants.*
import [group].shared.utils.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class [Entidade] private constructor(
    private var _id: Long,
    private var _externalId: UUID,
    private var _[campo]: String,
    private var _[campoOpcional]: String?,
    [SE FOR ATIVO/INATIVO]
    private var _status: [Nome]StatusEnum,
    private var _createdAt: LocalDateTime,
    private var _updatedAt: LocalDateTime,
    [SE CONFIRMOU SOFT DELETE]
    private var _deletedAt: LocalDateTime?
) {
    // Getters públicos (sem setters externos)
    val id: Long get() = _id
    val externalId: UUID get() = _externalId
    val [campo]: String get() = _[campo]
    val [campoOpcional]: String? get() = _[campoOpcional]
    [SE FOR ATIVO/INATIVO]
    val status: [Nome]StatusEnum get() = _status
    val createdAt: LocalDateTime get() = _createdAt
    val updatedAt: LocalDateTime get() = _updatedAt
    [SE CONFIRMOU SOFT DELETE]
    val deletedAt: LocalDateTime? get() = _deletedAt
    val isDeleted: Boolean get() = _deletedAt != null
    
    companion object {
        // Factory method para criar novo (id = 0 para auto-increment)
        fun createNew(
            [campo]: String,
            [campoOpcional]: String? = null
        ): [Entidade] {
            validate[Campo]([campo])
            [campoOpcional]?.let { validate[CampoOpcional](it) }
            
            val now = LocalDateTime.now()
            
            return [Entidade](
                _id = 0L, // Banco gerará o ID
                _externalId = UUID.randomUUID(),
                _[campo] = [campo].trim(),
                _[campoOpcional] = [campoOpcional]?.trim(),
                [SE FOR ATIVO/INATIVO]
                _status = [Nome]StatusEnum.ACTIVE,
                _createdAt = now,
                _updatedAt = now,
                [SE CONFIRMOU SOFT DELETE]
                _deletedAt = null
            )
        }
        
        // Factory method para reconstituir do banco
        fun reconstruct(
            id: Long,
            externalId: UUID,
            [campo]: String,
            [campoOpcional]: String?,
            [SE FOR ATIVO/INATIVO]
            status: [Nome]StatusEnum,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
            [SE CONFIRMOU SOFT DELETE]
            deletedAt: LocalDateTime?
        ): [Entidade] {
            // Validação para evitar uso indevido
            if (id <= 0) {
                throw [Contexto]ValidationException(
                    "ID deve ser maior que zero para reconstituir entidade"
                )
            }
            
            return [Entidade](
                _id = id,
                _externalId = externalId,
                _[campo] = [campo],
                _[campoOpcional] = [campoOpcional],
                [SE FOR ATIVO/INATIVO]
                _status = status,
                _createdAt = createdAt,
                _updatedAt = updatedAt,
                [SE CONFIRMOU SOFT DELETE]
                _deletedAt = deletedAt
            )
        }
        
        // Validações com exceções customizadas
        private fun validate[Campo](value: String) {
            if (value.isBlank()) {
                throw [Contexto]ValidationException("[Campo] não pode estar vazio")
            }
            
            if (value.length > [Contexto]Constants.MAX_[CAMPO]_LENGTH) {
                throw [Contexto]ValidationException(
                    "[Campo] não pode ter mais de ${[Contexto]Constants.MAX_[CAMPO]_LENGTH} caracteres"
                )
            }
        }
    }
    
    // Comportamentos que modificam estado
    fun update[Campo](new[Campo]: String) {
        validate[Campo](new[Campo])
        
        _[campo] = new[Campo].trim()
        _updatedAt = LocalDateTime.now()
    }
    
    [SE FOR ATIVO/INATIVO]
    // Para ativo/inativo - pode mudar várias vezes
    fun activate() {
        if (_status.isActive()) {
            throw [Contexto]BusinessRuleException("Registro já está ativo")
        }
        
        _status = [Nome]StatusEnum.ACTIVE
        _updatedAt = LocalDateTime.now()
    }
    
    fun deactivate() {
        if (!_status.isActive()) {
            throw [Contexto]BusinessRuleException("Registro já está inativo")
        }
        
        _status = [Nome]StatusEnum.INACTIVE
        _updatedAt = LocalDateTime.now()
    }
    
    [SE CONFIRMOU SOFT DELETE]
    // Soft delete é DEFINITIVO - sem restore!
    fun delete() {
        if (_deletedAt != null) {
            throw [Contexto]BusinessRuleException("Registro já foi deletado")
        }
        
        _deletedAt = LocalDateTime.now()
        _updatedAt = LocalDateTime.now()
    }
    // NÃO implementar restore() - soft delete é definitivo
    
    // equals e hashCode baseados no externalId
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is [Entidade]) return false
        return _externalId == other._externalId
    }
    
    override fun hashCode(): Int = _externalId.hashCode()
    
    override fun toString(): String {
        return "[Entidade](id=$_id, externalId=$_externalId, " +
               "[campo]=${_[campo].mask[Tipo]()}, " +
               [SE FOR ATIVO/INATIVO]
               "status=${_status.description}, " +
               [SE CONFIRMOU SOFT DELETE]
               "isDeleted=$isDeleted)"
    }
}

// Value Objects como data classes - baseado no projeto atual
data class [Contexto]Info(
    val [campo]: String,
    val [campoOpcional]: String?,
    val [campoData]: LocalDate,
    val [campoNumerico]: Int
) {
    init {
        require([campo].isNotBlank()) { "[Campo] não pode estar vazio" }
        require([campoNumerico] > 0) { "[Campo] deve ser positivo" }
    }
}
```

#### Template para Banco NÃO-RELACIONAL (Apenas UUID)
```kotlin
// Similar ao template acima, mas:
// - Apenas _id: UUID (sem _externalId)
// - Factory createNew() gera UUID diretamente
// - Sem validação de id > 0 no reconstruct
// - equals/hashCode usam _id diretamente
```

### 4. ENUMS DE DOMÍNIO

```kotlin
package [group].domain.[contexto].enum

import [group].shared.exception.Invalid[Contexto]EnumException

enum class [Nome]StatusEnum(
    val code: String,
    val description: String
) {
    ACTIVE("A", "Ativo"),
    INACTIVE("I", "Inativo"),
    SUSPENDED("S", "Suspenso");
    
    companion object {
        // Factory que lança exceção
        fun fromValue(value: String): [Nome]StatusEnum {
            return fromValueOrNull(value)
                ?: throw Invalid[Contexto]EnumException(value, "[Nome]StatusEnum")
        }
        
        // Factory que retorna null
        fun fromValueOrNull(value: String): [Nome]StatusEnum? {
            return values().firstOrNull { 
                it.name.equals(value, ignoreCase = true) || 
                it.code.equals(value, ignoreCase = true)
            }
        }
        
        fun fromCode(code: String): [Nome]StatusEnum {
            return values().firstOrNull { it.code == code }
                ?: throw Invalid[Contexto]EnumException(code, "[Nome]StatusEnum")
        }
    }
    
    // Comportamentos básicos (sem overengineering)
    fun isActive() = this == ACTIVE
    fun isInactive() = this == INACTIVE
    fun isSuspended() = this == SUSPENDED
    
    fun canTransitionTo(target: [Nome]StatusEnum): Boolean {
        return when (this) {
            ACTIVE -> target in listOf(INACTIVE, SUSPENDED)
            INACTIVE -> target == ACTIVE
            SUSPENDED -> target in listOf(ACTIVE, INACTIVE)
        }
    }
    
    fun isTerminal() = this == INACTIVE
}
```

### 5. REPOSITORY INTERFACES

```kotlin
package [group].domain.[contexto].repository

import [group].domain.[contexto].entity.[Entidade]
import [group].shared.dto.[Entidade]Filter
import [group].shared.dto.Page
import java.util.UUID

interface [Entidade]Repository {
    // Apenas save (sem update separado)
    fun save(entity: [Entidade]): [Entidade]
    
    // Finds básicos
    [PARA BANCO RELACIONAL]
    fun findById(id: Long): [Entidade]?
    fun findByExternalId(externalId: UUID): [Entidade]?
    
    [PARA BANCO NÃO-RELACIONAL]
    fun findById(id: UUID): [Entidade]?
    
    // Finds com lógica
    fun findActiveById(id: Long): [Entidade]?
    fun existsById(id: Long): Boolean
    fun existsByExternalId(externalId: UUID): Boolean
    
    // Finds específicos do domínio
    fun findBy[CampoUnico]([campo]: String): [Entidade]?
    fun findActiveBy[Campo]([campo]: String): List<[Entidade]>
    
    // Query complexa com DTO
    fun findByFilter(filter: [Entidade]Filter): List<[Entidade]>
    
    // Paginação
    fun findAllActive(
        page: Int,
        size: Int,
        sortBy: String = "createdAt",
        sortDirection: String = "DESC"
    ): Page<[Entidade]>
    
    // Sem métodos de delete! (gerenciado por Domain Service se tiver soft delete)
}
```

### 6. DOMAIN SERVICES

#### Service para Soft Delete (APENAS se confirmado necessidade)
```kotlin
package [group].domain.[contexto].service

import [group].domain.[contexto].entity.[Entidade]
import [group].domain.[contexto].repository.[Entidade]Repository
import [group].shared.exception.[Contexto]BusinessRuleException

/**
 * Domain Service responsável por gerenciar soft delete de [Entidade].
 * Soft delete é uma operação DEFINITIVA para fins de auditoria.
 * NÃO existe operação de restore.
 */
class [Entidade]DeletionDomainService(
    private val [entidade]Repository: [Entidade]Repository,
    [SE PRECISAR VALIDAR DEPENDÊNCIAS]
    private val [outraEntidade]Repository: [OutraEntidade]Repository
) {
    
    /**
     * Executa soft delete após validações de negócio.
     * Esta operação é DEFINITIVA e não pode ser revertida.
     */
    fun delete[Entidade]([entidade]: [Entidade]) {
        // Validações complexas antes de deletar
        validateCanDelete([entidade])
        
        // Marca como deletado (definitivo)
        [entidade].delete()
        
        // Persiste a mudança
        [entidade]Repository.save([entidade])
    }
    
    private fun validateCanDelete([entidade]: [Entidade]) {
        // Validação de estado
        if ([entidade].status.isActive()) {
            throw [Contexto]BusinessRuleException(
                "Não é possível deletar [entidade] com status ativo"
            )
        }
        
        [SE TIVER DEPENDÊNCIAS]
        // Validação de dependências
        val hasDependencies = [outraEntidade]Repository
            .existsBy[Entidade]Id([entidade].id)
            
        if (hasDependencies) {
            throw [Contexto]BusinessRuleException(
                "[Entidade] possui dependências e não pode ser deletada"
            )
        }
        
        // Outras validações específicas do negócio
    }
}
```

#### Service para Lógica Complexa
```kotlin
package [group].domain.[contexto].service

// Usar quando:
// - Lógica envolve múltiplas entidades
// - Validações precisam consultar repositories
// - Cálculos complexos entre agregados

class [Contexto]DomainService(
    private val [entidade]Repository: [Entidade]Repository,
    private val [calculo]Service: [Calculo]Service
) {
    
    fun execute[OperacaoComplexa](
        [parametros]
    ): [Resultado] {
        // 1. Validações que requerem repository
        val existing = [entidade]Repository.findBy[Campo]([valor])
        if (existing != null && existing.id != [id]) {
            throw [Contexto]BusinessRuleException(
                "[Campo] já está em uso"
            )
        }
        
        // 2. Lógica complexa
        val resultado = [calcular/processar]
        
        // 3. Retornar resultado
        return resultado
    }
}
```

## 🎯 PROCESSO DE GERAÇÃO

1. **Confirme tipo de banco** (relacional vs não-relacional)
2. **Diferencie soft delete de ativo/inativo** para cada entidade
3. **Identifique enums** com sufixo Enum
4. **Avalie Domain Services** necessários
5. **Gere arquivos**:
   - Exceções específicas (incluindo enums)
   - Entities com ID apropriado ao banco
   - Enums com dois factory methods
   - Repository sem delete (se tiver soft delete)
   - Domain Services apenas para soft delete confirmado
6. **Use artifacts separados** por contexto

## ⚠️ PONTOS DE ATENÇÃO

### Soft Delete vs Ativo/Inativo
- **Ativo/Inativo**: Estado mutável, pode alternar (activate/deactivate)
- **Soft Delete**: DEFINITIVO, para auditoria, SEM restore()
- **Não confunda**: Maioria dos casos é ativo/inativo, não soft delete

### Entities
- **ID duplo para relacionais**: id (Long) + externalId (UUID)
- **Validação no reconstruct**: id > 0 para evitar mau uso
- **hashCode/equals**: usar externalId em relacionais
- **Soft delete**: SEM método restore()

### Enums
- **Sempre sufixo Enum**: OrderStatusEnum, UserTypeEnum
- **Dois factory methods**: fromValue() e fromValueOrNull()
- **Exceção customizada**: Invalid[Contexto]EnumException
- **Comportamentos básicos**: sem overengineering

### Repository
- **Sem update**: apenas save()
- **Sem delete**: se tiver soft delete
- **DTOs Filter**: para queries complexas
- **Retorno nullable**: tratamento no use case

### Domain Services
- **Soft delete**: validações + execução DEFINITIVA
- **Lógicas complexas**: quando não cabem em factory
- **Não é CRUD**: deve ter regra de negócio real

---

### FEEDBACK
<!-- Registro de melhorias durante uso -->

### NOTAS DE VERSÃO

#### v1.0.0
- Versão inicial do DOMAIN-LAYER
- Implementação de entidades, value objects e interfaces de repositório
- Padrões para domain services e regras de negócio
