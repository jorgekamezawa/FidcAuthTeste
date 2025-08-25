# 💾 PERSISTENCE-LAYER - CAMADA DE PERSISTÊNCIA (REPOSITORY)

---
id: persistence-layer
version: 1.0.0
requires: [meta-prompt, project-context, initial-setup, infrastructure-base, domain-layer, application-layer]
provides: [repository-implementations, persistence-entities, database-config, cache-config]
optional: false
---

## 🎯 SEU PAPEL

Você irá implementar a camada de persistência (módulo repository) seguindo Clean Architecture. Este módulo pertence à camada Infrastructure e é responsável por implementar os contratos de repository definidos no domínio, isolando completamente os detalhes de persistência e tecnologias de banco de dados.

## 📋 PRÉ-REQUISITOS

Antes de iniciar:
1. **Interfaces de repository** definidas no domain
2. **Entidades de domínio** já implementadas
3. **Banco de dados** identificado (PostgreSQL, MongoDB, Redis, etc)
4. **DDL do banco** se for relacional

**IMPORTANTE**: Este prompt pode ser usado de forma independente. A IA deve:
1. Verificar se há contexto/entidades definidas anteriormente
2. Se não houver, solicitar documentação das entidades e repositories
3. Trabalhar com o que for fornecido

## 🔄 FLUXO DE GERAÇÃO

### 1. Identificação das Implementações Necessárias
```
"Vou verificar quais repositories precisam ser implementados.

[VERIFICAR CONTEXTO DA CONVERSA]
- Procurando por interfaces de repository no domain...
- Procurando por entidades de domínio...
- Procurando por tipo de banco definido...

[SE ENCONTROU CONTEXTO]
Baseado no contexto, identifiquei estas implementações necessárias:

1. **[Entidade]Repository**
   - Banco: [PostgreSQL/Redis/MongoDB]
   - Interface: [Nome]Repository
   - Operações: [listar principais]

2. **[Entidade]Repository**
   - Banco: [tipo]
   - Interface: [Nome]Repository
   - Operações: [listar]

[SE NÃO ENCONTROU CONTEXTO]
Não encontrei contexto ou repositories definidos.

Para implementar a camada de persistência, preciso que você forneça:

1. **Interfaces de repository** (do domain):
   - Nome da interface
   - Métodos a implementar
   - Entidade relacionada

2. **Tipo de persistência**:
   - PostgreSQL (com JPA)
   - Redis (cache/estado)
   - MongoDB (documentos)
   - Outro

3. **Entidades de domínio**:
   - Campos e tipos
   - ID duplo (para relacionais)?

Exemplo:
```
Interface: CustomerRepository
Entidade: Customer (id: Long, externalId: UUID, name: String, email: String)
Banco: PostgreSQL
Métodos: save, findById, findByEmail
```

Qual repository gostaria de implementar primeiro?"
```

### 2. Configuração de Persistência
```
"Para o(s) banco(s) identificado(s), vou criar:

[SE POSTGRESQL/JPA]
**Configurações JPA**:
- JpaConfig com configurações base
- Entities JPA separadas das de domínio
- Mappers bidirecionais

[SE REDIS]
**Configurações Redis**:
- RedisConfig com serialização JSON
- Entities Redis com @JsonFormat
- Health indicator para Redis

[SE MONGODB]
**Configurações MongoDB**:
- MongoConfig com conversores
- Documents com @Document
- Índices necessários

Confirma estas configurações?"
```

### 3. Estratégia de Mapeamento
```
"Para mapear entre domain e persistência, vou usar:

**Extension Functions** (padrão Kotlin):
- toJpaEntity() / toRedisEntity() / toDocument()
- toDomainEntity()
- Localizadas no arquivo da entity de persistência

**Tratamento de Enums**:
- Armazenar como String (name do enum)
- Conversão segura com fromValue()

**Datas e Timestamps**:
- @JsonFormat para Redis
- Configuração JPA para timestamps

Concorda com esta abordagem?"
```

## 📁 ESTRUTURAS A SEREM GERADAS

### 1. ESTRUTURA DO REPOSITORY

```
repository/
└── src/main/kotlin/[package/path]/repository/
    ├── config/
    │   ├── JpaConfig.kt              [SE USAR JPA]
    │   ├── RedisConfig.kt            [SE USAR REDIS]
    │   └── MongoConfig.kt            [SE USAR MONGODB]
    ├── [tipo-banco]/                 [jpa/redis/mongo]
    │   └── [contexto]/
    │       ├── entity/               [ou document para mongo]
    │       │   └── [Entidade][Tipo]Entity.kt
    │       ├── exception/
    │       │   └── [Contexto]RepositoryException.kt
    │       ├── impl/
    │       │   └── [Entidade]RepositoryImpl.kt
    │       └── repository/           [SE JPA - Spring Data]
    │           └── [Entidade]JpaRepository.kt
    └── health/                       [SE REDIS]
        └── RedisHealthIndicator.kt
```

### 2. CONFIGURAÇÕES POR TIPO DE BANCO

#### Para JPA/PostgreSQL

**JpaConfig.kt**
```kotlin
package [package].repository.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackages = ["[package].repository.jpa"])
@EnableJpaAuditing
@EnableTransactionManagement
class JpaConfig
```

**JPA Entity**
```kotlin
package [package].repository.jpa.[contexto].entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "[nome_tabela]",
    schema = "[nome_schema]",
    indexes = [
        Index(name = "idx_[tabela]_uuid", columnList = "uuid"),
        [OUTROS ÍNDICES DEFINIDOS NO DDL]
    ]
)
class [Entidade]JpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long = 0,
    
    @Column(name = "uuid", nullable = false, unique = true, columnDefinition = "UUID")
    var uuid: UUID = UUID.randomUUID(),
    
    @Column(name = "[campo]", nullable = false, columnDefinition = "TEXT")
    var [campo]: String = "",
    
    @Column(name = "[campo_opcional]", columnDefinition = "TEXT")
    var [campoOpcional]: String? = null,
    
    [SE TIVER ENUM]
    @Column(name = "[campo_enum]", nullable = false, columnDefinition = "TEXT")
    var [campoEnum]: String = "",
    
    [SE TIVER RELACIONAMENTO]
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "[entidade]_id")
    var [entidade]: [Entidade]JpaEntity? = null,
    
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    
    [SE TIVER SOFT DELETE]
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null
)

// Mappers como extension functions
fun [Entidade].toJpaEntity(): [Entidade]JpaEntity {
    return [Entidade]JpaEntity().apply {
        this.id = this@toJpaEntity.id
        this.uuid = this@toJpaEntity.externalId
        this.[campo] = this@toJpaEntity.[campo]
        this.[campoOpcional] = this@toJpaEntity.[campoOpcional]
        [SE TIVER ENUM]
        this.[campoEnum] = this@toJpaEntity.[campoEnum].name
        this.createdAt = this@toJpaEntity.createdAt
        this.updatedAt = this@toJpaEntity.updatedAt
        [SE TIVER SOFT DELETE]
        this.deletedAt = this@toJpaEntity.deletedAt
    }
}

fun [Entidade]JpaEntity.toDomainEntity(): [Entidade] {
    [SE TIVER ENUM]
    val [campoEnum] = [Nome]Enum.fromValue(this.[campoEnum])
    
    return [Entidade].reconstruct(
        id = this.id,
        externalId = this.uuid,
        [campo] = this.[campo],
        [campoOpcional] = this.[campoOpcional],
        [SE TIVER ENUM]
        [campoEnum] = [campoEnum],
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        [SE TIVER SOFT DELETE]
        deletedAt = this.deletedAt
    )
}
```

**Spring Data Repository**
```kotlin
package [package].repository.jpa.[contexto].repository

import [package].repository.jpa.[contexto].entity.[Entidade]JpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface [Entidade]JpaRepository : JpaRepository<[Entidade]JpaEntity, Long> {
    
    fun findByUuid(uuid: UUID): [Entidade]JpaEntity?
    
    fun existsByUuid(uuid: UUID): Boolean
    
    [SE TIVER CAMPOS ÚNICOS]
    fun findBy[Campo]([campo]: String): [Entidade]JpaEntity?
    
    [SE TIVER SOFT DELETE]
    @Query("SELECT e FROM [Entidade]JpaEntity e WHERE e.id = :id AND e.deletedAt IS NULL")
    fun findActiveById(@Param("id") id: Long): [Entidade]JpaEntity?
    
    [QUERIES CUSTOMIZADAS CONFORME INTERFACE DO DOMAIN]
}
```

#### Para Redis

**RedisConfig.kt**
```kotlin
package [package].repository.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper
    ): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        // JSON serialization for interoperability
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = createJsonSerializer(objectMapper)
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = createJsonSerializer(objectMapper)

        template.afterPropertiesSet()
        return template
    }

    private fun createJsonSerializer(objectMapper: ObjectMapper): Jackson2JsonRedisSerializer<Any> {
        return Jackson2JsonRedisSerializer(objectMapper, Any::class.java)
    }
}
```

**Redis Entity**
```kotlin
package [package].repository.redis.[contexto].entity

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class [Entidade]RedisEntity(
    val id: UUID,
    val [campo]: String,
    val [campoOpcional]: String?,
    val [campoEnum]: String,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    val [campoDate]: LocalDate?,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val updatedAt: LocalDateTime
)

// Mappers
fun [Entidade].toRedisEntity(): [Entidade]RedisEntity {
    return [Entidade]RedisEntity(
        id = this.externalId,
        [campo] = this.[campo],
        [campoOpcional] = this.[campoOpcional],
        [campoEnum] = this.[campoEnum].name,
        [campoDate] = this.[campoDate],
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun [Entidade]RedisEntity.toDomainEntity(): [Entidade] {
    val [campoEnum] = [Nome]Enum.fromValue(this.[campoEnum])
    
    return [Entidade].reconstruct(
        id = 0L, // Redis não tem ID sequencial
        externalId = this.id,
        [campo] = this.[campo],
        [campoOpcional] = this.[campoOpcional],
        [campoEnum] = [campoEnum],
        [campoDate] = this.[campoDate],
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
```

### 3. REPOSITORY IMPLEMENTATIONS

#### JPA Repository Implementation
```kotlin
package [package].repository.jpa.[contexto].impl

import [package].domain.[contexto].entity.[Entidade]
import [package].domain.[contexto].repository.[Entidade]Repository
import [package].repository.jpa.[contexto].entity.*
import [package].repository.jpa.[contexto].exception.[Contexto]RepositoryException
import [package].repository.jpa.[contexto].repository.[Entidade]JpaRepository
import [package].shared.dto.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class [Entidade]RepositoryImpl(
    private val jpaRepository: [Entidade]JpaRepository
) : [Entidade]Repository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun save(entity: [Entidade]): [Entidade] {
        logger.debug("Saving [entidade]: id=${entity.id}, externalId=${entity.externalId}")
        
        try {
            val jpaEntity = if (entity.id > 0) {
                // Update existing
                val existing = jpaRepository.findById(entity.id)
                    .orElseThrow { 
                        IllegalStateException("[Entidade] not found for update: id=${entity.id}")
                    }
                entity.toJpaEntity().apply { 
                    this.id = existing.id // Preserve ID
                }
            } else {
                // Create new
                entity.toJpaEntity()
            }
            
            val saved = jpaRepository.save(jpaEntity)
            logger.debug("[Entidade] saved successfully: id=${saved.id}")
            
            return saved.toDomainEntity()
            
        } catch (e: IllegalStateException) {
            throw e // Business exceptions propagate unchanged
        } catch (e: Exception) {
            logger.error("Error saving [entidade]: ${e.message}", e)
            throw [Contexto]RepositoryException(
                "Failed to save [entidade]",
                e
            )
        }
    }

    override fun findById(id: Long): [Entidade]? {
        logger.debug("Finding [entidade] by id: $id")
        
        return try {
            jpaRepository.findById(id)
                .map { it.toDomainEntity() }
                .orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding [entidade] by id: ${e.message}", e)
            throw [Contexto]RepositoryException(
                "Failed to find [entidade] by id",
                e
            )
        }
    }

    override fun findByExternalId(externalId: UUID): [Entidade]? {
        logger.debug("Finding [entidade] by externalId: $externalId")
        
        return try {
            jpaRepository.findByUuid(externalId)
                ?.toDomainEntity()
        } catch (e: Exception) {
            logger.error("Error finding [entidade] by externalId: ${e.message}", e)
            throw [Contexto]RepositoryException(
                "Failed to find [entidade] by externalId",
                e
            )
        }
    }

    override fun findAllActive(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): Page<[Entidade]> {
        logger.debug("Finding active [entidades]: page=$page, size=$size")
        
        try {
            val sort = Sort.by(
                if (sortDirection == "ASC") Sort.Direction.ASC else Sort.Direction.DESC,
                sortBy
            )
            val pageable = PageRequest.of(page, size, sort)
            
            [SE TIVER SOFT DELETE]
            val result = jpaRepository.findAllByDeletedAtIsNull(pageable)
            [SE NÃO TIVER SOFT DELETE]
            val result = jpaRepository.findAll(pageable)
            
            return Page(
                content = result.content.map { it.toDomainEntity() },
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                currentPage = result.number,
                pageSize = result.size,
                hasNext = result.hasNext(),
                hasPrevious = result.hasPrevious()
            )
        } catch (e: Exception) {
            logger.error("Error finding active [entidades]: ${e.message}", e)
            throw [Contexto]RepositoryException(
                "Failed to find active [entidades]",
                e
            )
        }
    }
}
```

#### Redis Repository Implementation
```kotlin
package [package].repository.redis.[contexto].impl

import [package].domain.[contexto].entity.[Entidade]
import [package].domain.[contexto].repository.[Entidade]Repository
import [package].repository.redis.[contexto].entity.*
import [package].repository.redis.[contexto].exception.RedisRepositoryException
import [package].usecase.[contexto].configprovider.[Contexto]ConfigProvider
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.TimeUnit

@Repository
class [Entidade]RepositoryImpl(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
    private val configProvider: [Contexto]ConfigProvider
) : [Entidade]Repository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun saveWithTTL(entity: [Entidade], ttlMinutes: Int): [Entidade] {
        val key = generateKey(entity)
        val redisEntity = entity.toRedisEntity()
        
        logger.debug("Saving to Redis: key=$key, ttl=${ttlMinutes}min")
        
        try {
            redisTemplate.opsForValue().set(
                key, 
                redisEntity, 
                ttlMinutes.toLong(), 
                TimeUnit.MINUTES
            )
            logger.debug("Successfully saved to Redis")
            return entity
            
        } catch (e: Exception) {
            logger.error("Error saving to Redis: key=$key", e)
            throw RedisRepositoryException(
                "Failed to save [entidade]",
                e
            )
        }
    }

    override fun findByKey([params]): [Entidade]? {
        val key = generateKey([params])
        
        try {
            val value = redisTemplate.opsForValue().get(key) ?: return null
            
            // Handle both direct object and Map (JSON) deserialization
            val redisEntity = when (value) {
                is [Entidade]RedisEntity -> value
                is Map<*, *> -> deserializeFromMap(value, key)
                else -> {
                    logger.warn("Unexpected type in Redis: key=$key, type=${value::class.java}")
                    null
                }
            }
            
            return redisEntity?.toDomainEntity()
            
        } catch (e: Exception) {
            logger.error("Error retrieving from Redis: key=$key", e)
            throw RedisRepositoryException(
                "Failed to retrieve [entidade]",
                e
            )
        }
    }

    override fun updatePreservingTTL(entity: [Entidade]): [Entidade] {
        val key = generateKey(entity)
        
        try {
            val ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES)
            
            if (ttl <= 0) {
                throw IllegalStateException("[Entidade] does not exist or has expired")
            }
            
            val redisEntity = entity.toRedisEntity()
            redisTemplate.opsForValue().set(key, redisEntity, ttl, TimeUnit.MINUTES)
            
            logger.debug("Updated preserving TTL: key=$key, ttl=${ttl}min")
            return entity
            
        } catch (e: IllegalStateException) {
            throw e // Business exceptions propagate
        } catch (e: Exception) {
            logger.error("Error updating in Redis: key=$key", e)
            throw RedisRepositoryException(
                "Failed to update [entidade]",
                e
            )
        }
    }

    private fun generateKey([params]): String {
        val prefix = configProvider.getRedisKeyPrefix()
        return "$prefix:[param1]:[param2]".lowercase()
    }

    private fun deserializeFromMap(value: Map<*, *>, key: String): [Entidade]RedisEntity? {
        return try {
            val json = objectMapper.writeValueAsString(value)
            objectMapper.readValue(json, [Entidade]RedisEntity::class.java)
        } catch (e: Exception) {
            logger.error("Error deserializing from Redis: key=$key", e)
            null
        }
    }
}
```

### 4. EXCEÇÕES ESPECÍFICAS

```kotlin
package [package].repository.[tipo].[contexto].exception

import [package].shared.exception.InfrastructureException

class [Contexto]RepositoryException(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "[TipoBanco]Repository",
    message = message,
    cause = cause
)

// Para Redis específico
class RedisRepositoryException(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "Redis",
    message = message,
    cause = cause
)
```

### 5. HEALTH INDICATORS (SE REDIS)

```kotlin
package [package].repository.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.stereotype.Component

@Component
class RedisHealthIndicator(
    private val redisConnectionFactory: RedisConnectionFactory
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val connection = redisConnectionFactory.connection
            connection.ping()
            connection.close()
            
            Health.up()
                .withDetail("type", "Redis")
                .build()
                
        } catch (ex: Exception) {
            Health.down()
                .withDetail("type", "Redis")
                .withException(ex)
                .build()
        }
    }
}
```

## 🎯 PROCESSO DE GERAÇÃO

1. **Verifique contexto existente** ou solicite documentação
2. **Identifique tipo de banco** para cada repository
3. **Configure persistência** apropriada
4. **Implemente ordem**:
   - Configurações do banco
   - Entities/Documents de persistência
   - Mappers bidirecionais
   - Repository implementations
   - Exceções específicas
5. **Use artifacts separados** por tipo de banco
6. **Teste compilação** mentalmente

## ⚠️ PONTOS DE ATENÇÃO

### Separação de Concerns
- **Entities de persistência**: Separadas das de domínio
- **Sem lógica de negócio**: Apenas persistência
- **Mappers isolados**: Como extension functions
- **Configurações coesas**: Por tipo de banco

### JPA Específico
- **GenerationType.IDENTITY**: Para PostgreSQL
- **Lazy loading**: Para relacionamentos
- **@Transactional**: Apenas em métodos que modificam
- **columnDefinition**: TEXT para strings

### Redis Específico
- **@JsonFormat**: Em todas as datas
- **Serialização JSON**: Para interoperabilidade
- **TTL explícito**: Métodos específicos
- **Deserialização flexível**: Map ou objeto

### Tratamento de Erros
- **IllegalStateException**: Problemas de fluxo
- **Repository exceptions**: Erros de infra
- **Logging apropriado**: Debug para ops, error para falhas
- **Null como ausência**: Não exception

### Performance
- **Índices**: Conforme DDL definido
- **Fetch lazy**: Por padrão em JPA
- **Paginação**: Com Page DTO
- **Cache**: Gerenciado pelo Spring

---

### FEEDBACK
<!-- Registro de melhorias durante uso -->

### NOTAS DE VERSÃO

#### v1.0.0
- Versão inicial do PERSISTENCE-LAYER
- Implementação de repositories com JPA e Redis
- Padrões para mapeamento entre entidades de domínio e persistência
