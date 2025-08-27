# 🎭 APPLICATION-LAYER - CAMADA DE APLICAÇÃO (USE CASES)

---
id: application-layer
version: 1.0.0
requires: [meta-prompt, project-context, initial-setup, infrastructure-base, domain-layer]
provides: [use-cases, application-services, dtos, config-providers]
optional: false
---

## 🎯 SEU PAPEL

Você irá implementar a camada Application que orquestra fluxos entre domain e infrastructure. Esta camada contém use cases, application services, DTOs específicos e interfaces de configuração, sem regras de negócio do domínio.

## 📋 PRÉ-REQUISITOS

Antes de iniciar:
1. **Fluxos de negócio** mapeados no PROJECT-CONTEXT
2. **Entidades de domínio** já implementadas
3. **Integrações externas** identificadas
4. **Configurações necessárias** mapeadas

## 🔄 FLUXO DE GERAÇÃO

**IMPORTANTE**: Este prompt pode ser usado de forma independente. A IA deve:
1. Primeiro verificar se há fluxos/documentação no contexto atual
2. Se não houver, solicitar a documentação do fluxo específico
3. Trabalhar com o que for fornecido, seja do PROJECT-CONTEXT ou documentação avulsa

### 1. Identificação dos Use Cases
```
"Vou verificar os fluxos de negócio para identificar os use cases.

[VERIFICAR CONTEXTO DA CONVERSA]
- Procurando fluxos mapeados anteriormente...
- Procurando documentação de fluxos já fornecida...

[SE ENCONTROU FLUXOS]
Baseado nos fluxos mapeados, identifiquei estes use cases principais:

[SE NÃO ENCONTROU FLUXOS]
Não encontrei fluxos de negócio mapeados no contexto atual.

Para prosseguir, preciso da documentação do(s) fluxo(s) que você 
deseja implementar. Por favor, forneça:

1. Nome do fluxo
2. Trigger (como é iniciado)
3. Integrações necessárias
4. Entidades envolvidas
5. Regras de negócio
6. Resultado esperado

Você pode fornecer no formato que foi definido no PROJECT-CONTEXT
ou em qualquer formato de documentação que tenha.

Qual fluxo gostaria de implementar primeiro?"

[APÓS TER OS FLUXOS]
Para cada fluxo identificado, criarei:

1. [Ação][Entidade]UseCase (ex: CreateOrderUseCase)
   - Fluxo: [nome do fluxo mapeado]
   - Responsabilidade: [o que orquestra]
   - Integrações: [services que usa]

2. [Ação][Contexto]UseCase
   - Fluxo: [nome do fluxo]
   - Responsabilidade: [orquestração]
   - Dependências: [repositories, services]

Cada use case terá:
- Interface com método execute()
- Input/Output DTOs específicos
- Implementação orquestrando o fluxo

Confirma estes use cases?"
```

### 2. Application Services Necessários
```
"Para os use cases, precisaremos destes Application Services:

INTERFACES (implementação na infra):
- [Sistema]Service: Integração com [sistema externo]
- [Recurso]GatewayService: Acesso a [recurso AWS/externo]

CLASSES CONCRETAS (lógica da aplicação):
- [Contexto]DiscoveryService: [lógica específica]
- [Entidade]ValidationService: [validações complexas]

Application Services concretos são para lógica que:
- É específica da aplicação (não do domínio)
- Não pertence a nenhuma entidade
- É reutilizada entre use cases

Confirma estes services?"
```

### 3. Config Providers
```
"Identifiquei estas configurações que precisam abstração:

- [Contexto]ConfigProvider:
  - get[Propriedade](): tipo
  - get[OutraPropriedade](): tipo

- [Recurso]ConfigProvider:
  - get[Configuração](): tipo

Config Providers são interfaces no usecase.
A implementação fica no módulo application.

Confirma estas configurações?"
```

## 📁 ESTRUTURAS A SEREM GERADAS

### 1. ESTRUTURA DO USECASE

```
usecase/
└── src/main/kotlin/[group/path]/usecase/
    └── [contexto]/
        ├── [Ação][Entidade]UseCase.kt          // Interface
        ├── impl/
        │   └── [Ação][Entidade]UseCaseImpl.kt  // Implementação
        ├── configprovider/
        │   ├── [Contexto]ConfigProvider.kt
        │   └── [Recurso]ConfigProvider.kt
        ├── dto/
        │   ├── input/
        │   │   └── [Ação][Entidade]Input.kt
        │   ├── output/
        │   │   └── [Ação][Entidade]Output.kt
        │   ├── params/                          // Para services
        │   │   └── [Operação]Params.kt
        │   └── result/                          // Retorno de services
        │       └── [Operação]Result.kt
        ├── exception/
        │   ├── [Contexto]ApplicationException.kt
        │   └── [Contexto]ValidationException.kt
        └── service/
            ├── [Sistema]Service.kt              // Interface (impl no external)
            ├── [Recurso]GatewayService.kt       // Interface (impl no external)
            └── [Contexto]ValidationService.kt   // Classe concreta
```

### 2. USE CASE PATTERN

#### Interface do Use Case
```kotlin
package [group].usecase.[contexto]

import [group].usecase.[contexto].dto.input.[Ação][Entidade]Input
import [group].usecase.[contexto].dto.output.[Ação][Entidade]Output

interface [Ação][Entidade]UseCase {
    fun execute(input: [Ação][Entidade]Input): [Ação][Entidade]Output
}
```

#### Implementação do Use Case (CLEAN CODE PATTERN)
```kotlin
package [group].usecase.[contexto].impl

// IMPORTS ORGANIZADOS: entidades específicas importadas, não wildcards no código
import [group].domain.[contexto].entity.*
import [group].domain.[contexto].repository.[Entidade]Repository
import [group].shared.exception.*
import [group].usecase.[contexto].*
import [group].usecase.[contexto].dto.input.[Ação][Entidade]Input
import [group].usecase.[contexto].dto.output.[Ação][Entidade]Output
import [group].usecase.[contexto].dto.params.*
import [group].usecase.[contexto].dto.result.*
import [group].usecase.[contexto].exception.*
import [group].usecase.[contexto].service.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class [Ação][Entidade]UseCaseImpl(
    private val [entidade]Repository: [Entidade]Repository,
    private val [sistema]Service: [Sistema]Service,
    private val [contexto]ValidationService: [Contexto]ValidationService,
    private val [contexto]ConfigProvider: [Contexto]ConfigProvider
) : [Ação][Entidade]UseCase {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // MÉTODO EXECUTE LIMPO: poucas linhas, sem comentários desnecessários
    @Transactional // Apenas se usar banco relacional com ACID
    override fun execute(input: [Ação][Entidade]Input): [Ação][Entidade]Output {
        logger.info("Executando [ação] de [entidade]: [campo]=${input.[campo]}")
        
        try {
            validateInput(input)
            val externalData = fetchExternalData(input.[campo])
            validateBusinessRules(input, externalData)
            
            val entity = createDomainEntity(input, externalData)
            val savedEntity = persistEntity(entity)
            
            notifyExternalSystems(savedEntity)
            
            logger.info("[Ação] de [entidade] concluída: id=${savedEntity.externalId}")
            return buildOutput(savedEntity)
            
        } catch (ex: InvalidSessionEnumException) {
            logger.warn("[Contexto] enum inválido - Enum: [EnumType], Valor: '${input.[campo]}'")
            throw InvalidInputException("[Campo] '${input.[campo]}' é incorreto. Valores aceitos: ${[Enum].getAcceptedValues()}")
        } catch (ex: BusinessException) {
            logger.warn("Erro de negócio em [ação][entidade]: ${ex.message}")
            throw ex
        } catch (ex: InfrastructureException) {
            logger.error("Erro de infraestrutura [${ex.component}]: ${ex.message}", ex)
            throw ex
        } catch (ex: Exception) {
            logger.error("Erro inesperado em [ação][entidade]", ex)
            throw [Contexto]ProcessingException("Erro ao processar [ação] de [entidade]", ex)
        }
    }
    
    // MÉTODOS NA ORDEM DE EXECUÇÃO: seguem exatamente a sequência do execute()
    private fun validateInput(input: [Ação][Entidade]Input) {
        // Validações que não são regras de domínio
    }
    
    private fun fetchExternalData(campo: String): [Sistema][Operação]Result {
        return [sistema]Service.fetch[Dados](
            [Sistema][Operação]Params(campo = campo)
        )
    }
    
    private fun validateBusinessRules(
        input: [Ação][Entidade]Input, 
        externalData: [Sistema][Operação]Result
    ) {
        [contexto]ValidationService.validate[Condição](
            input.[campo],
            externalData.[campo]
        )
    }
    
    private fun createDomainEntity(
        input: [Ação][Entidade]Input,
        externalData: [Sistema][Operação]Result
    ): [Entidade] {
        return [Entidade].createNew(
            [campo] = input.[campo],
            [dadosExternal] = mapToEntityData(externalData)
        )
    }
    
    private fun mapToEntityData(data: [Sistema][Operação]Result): [EntityData] {
        return [EntityData](
            [campo] = data.[campo],
            [outrosCampos] = data.[outrosCampos]
        )
    }
    
    private fun persistEntity(entity: [Entidade]): [Entidade] {
        return [entidade]Repository.save(entity)
    }
    
    private fun notifyExternalSystems(savedEntity: [Entidade]) {
        [sistema]Service.notify[Evento](
            [Sistema]Notify[Evento]Params(
                entityId = savedEntity.externalId
            )
        )
    }
    
    private fun buildOutput(savedEntity: [Entidade]): [Ação][Entidade]Output {
        return [Ação][Entidade]Output(
            [entidade]Id = savedEntity.externalId,
            [status] = savedEntity.status.name,
            [timestamp] = savedEntity.createdAt
        )
    }
}
```

### 3. DTOs (DATA TRANSFER OBJECTS)

#### Input/Output para Use Cases
```kotlin
// usecase/[contexto]/dto/input/
data class [Ação][Entidade]Input(
    val [campo]: String,
    val [campoOpcional]: String?,
    val [campoLista]: List<[Item]>,
    val [campoData]: LocalDate
    // Sem valores default - explicitação
    // NUNCA incluir correlationId - capturado pelo CorrelationIdFilter
)

// usecase/[contexto]/dto/output/
data class [Ação][Entidade]Output(
    val [entidade]Id: UUID,
    val [campo]: String,
    val [status]: String,
    val [timestamp]: LocalDateTime
)
```

#### Params/Result para Services
```kotlin
// usecase/[contexto]/dto/params/
data class [Sistema][Operação]Params(
    val [campo]: String,
    val [configuração]: Int,
    val [filtros]: List<String>
)

// usecase/[contexto]/dto/result/
data class [Sistema][Operação]Result(
    val [dadoRetornado]: String,
    val [status]: String,
    val [metadata]: Map<String, Any>?
)
```

#### Mappers como Extension Functions
```kotlin
// No arquivo do Result
fun [Sistema][Operação]Result.to[Entidade]Data(): [Entidade]Data {
    return [Entidade]Data(
        [campo] = this.[dadoRetornado],
        [outrosCampos] = processar(this.[metadata])
    )
}

// Mapper para múltiplos dados - extension function
fun [Sistema][Operação]Result.toOutputData(): Triple<[Tipo1]Data, [Tipo2]Data, List<[Tipo3]Data>> {
    return Triple(
        [Tipo1]Data(
            [campo] = this.[campo1],
            [outrosCampos] = this.[dados1]
        ),
        [Tipo2]Data(
            [campo] = this.[campo2],
            [outrosCampos] = this.[dados2]
        ),
        this.[lista].map {
            [Tipo3]Data(
                [campo] = it.[campo],
                [outrosCampos] = it.[dados]
            )
        }
    )
}

// Mapper complexo em classe separada (raro)
@Component
class [Contexto]ComplexMapper {
    fun map[Origem]To[Destino](
        origem: [Origem],
        contextData: [ContextData]
    ): [Destino] {
        // Lógica complexa de mapeamento
        // Validações durante mapeamento
        // Múltiplas transformações
    }
}
```

### 4. APPLICATION SERVICES

#### Interfaces (Implementação no External/Repository)
```kotlin
package [group].usecase.[contexto].service

// Interface para sistema externo
interface [Sistema]Service {
    fun fetch[Dados](params: [Sistema][Operação]Params): [Sistema][Operação]Result
    fun send[Comando](params: [Sistema][Comando]Params)
    fun validate[Algo](params: [Sistema]ValidationParams): Boolean
}

// Interface para gateway/infraestrutura
interface [Recurso]GatewayService {
    fun store[Recurso](data: ByteArray, metadata: Map<String, String>): String
    fun retrieve[Recurso](id: String): ByteArray?
    fun delete[Recurso](id: String)
}
```

#### Classes Concretas (Lógica da Aplicação)
```kotlin
package [group].usecase.[contexto].service

@Service
class [Contexto]ValidationService(
    private val [entidade]Repository: [Entidade]Repository,
    private val [configuração]ConfigProvider: [Configuração]ConfigProvider
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    /**
     * Valida [condição] que envolve múltiplos agregados.
     * Esta NÃO é uma regra de domínio pura, mas sim
     * uma validação específica da aplicação.
     */
    fun validate[Condição](
        [parametro1]: String,
        [parametro2]: String
    ) {
        logger.debug("Validando [condição]: [param1]=$[parametro1]")
        
        // Validação que precisa consultar múltiplos lugares
        val existing = [entidade]Repository.findBy[Campo]([parametro1])
        if (existing != null && existing.[campo] != [parametro2]) {
            throw [Contexto]ValidationException(
                "[Mensagem explicando a validação que falhou]"
            )
        }
        
        // Validação com configuração
        val maxAllowed = [configuração]ConfigProvider.getMax[Algo]()
        if ([calcular] > maxAllowed) {
            throw [Contexto]ValidationException(
                "[Algo] excede o máximo permitido de $maxAllowed"
            )
        }
    }
}

@Service
class [Contexto]DiscoveryService(
    private val [sistema1]Service: [Sistema1]Service,
    private val [sistema2]Service: [Sistema2]Service
) {
    /**
     * Lógica de descoberta específica da aplicação.
     * Combina dados de múltiplas fontes.
     */
    fun discover[Recurso](criteria: [Criteria]): [Discovery]Result {
        // Tenta primeiro no sistema 1
        val result1 = [sistema1]Service.find[Recurso](criteria.to[Sistema1]Params())
        if (result1 != null) {
            return [Discovery]Result(
                source = "SISTEMA1",
                data = result1.to[Discovery]Data()
            )
        }
        
        // Fallback para sistema 2
        val result2 = [sistema2]Service.find[Recurso](criteria.to[Sistema2]Params())
        return [Discovery]Result(
            source = "SISTEMA2",
            data = result2?.to[Discovery]Data()
        )
    }
}
```

### 5. CONFIG PROVIDERS

```kotlin
package [group].usecase.[contexto].configprovider

// Interface no usecase
interface [Contexto]ConfigProvider {
    fun get[Propriedade](): String
    fun get[Timeout]Seconds(): Int
    fun get[Limite](): Int
    fun is[Feature]Enabled(): Boolean
}

// Interface para configurações de outro contexto
interface [Recurso]ConfigProvider {
    fun get[Url](): String
    fun get[ApiKey](): String
    fun getMax[Tentativas](): Int
    fun get[CacheTtl]Minutes(): Int
}
```

### 6. EXCEÇÕES ESPECÍFICAS

```kotlin
package [group].usecase.[contexto].exception

import [group].shared.exception.BusinessException

// Exceções de aplicação (não são de domínio)
sealed class [Contexto]ApplicationException(
    message: String,
    cause: Throwable? = null
) : BusinessException(message, cause)

// Validações específicas da aplicação
class [Contexto]ValidationException(
    message: String
) : [Contexto]ApplicationException(message)

// Erros de integração específicos
class [Sistema]IntegrationException(
    message: String,
    cause: Throwable? = null
) : [Contexto]ApplicationException(
    "[Sistema] integration failed: $message", cause
)

// Erros de processamento
class [Contexto]ProcessingException(
    message: String,
    cause: Throwable? = null
) : [Contexto]ApplicationException(message, cause)
```

## 🎯 PROCESSO DE GERAÇÃO

1. **Identifique use cases** baseados nos fluxos
2. **Mapeie application services** necessários
3. **Defina config providers** para abstrair configurações
4. **Crie DTOs** específicos (Input/Output, Params/Result)
5. **Implemente use cases** orquestrando o fluxo
6. **Gere services concretos** para lógica de aplicação
7. **Use artifacts separados** por contexto/use case

## 🧹 PADRÕES DE CLEAN CODE OBRIGATÓRIOS

### Organização da Classe Use Case
1. **Método `execute()` SEMPRE primeiro**: Método principal deve ser o primeiro da classe
2. **Métodos na ordem de execução**: Métodos privados devem seguir exatamente a ordem que são chamados no `execute()`
3. **Poucas linhas no execute()**: Se exceder, extrair métodos com nomes descritivos
4. **Sem comentários desnecessários**: Código deve ser autoexplicativo

### Padrão de Imports
```kotlin
// ✅ CORRETO: Imports organizados
import [group].domain.[contexto].entity.*
import [group].usecase.[contexto].dto.input.[Ação][Entidade]Input
import [group].usecase.[contexto].exception.*

// ❌ ERRADO: Nomes completos no código
val userInfo = com.banco.fidc.auth.domain.session.entity.UserInfo(...)
```

### Tratamento de Exceções Específico
```kotlin
// ✅ CORRETO: Tratamento específico para enums inválidos
} catch (ex: Invalid[Contexto]EnumException) {
    logger.warn("[Contexto] enum inválido - Enum: [EnumType], Valor: '${input.[campo]}'")
    throw InvalidInputException("[Campo] '${input.[campo]}' é incorreto. Valores aceitos: ${[Enum].getAcceptedValues()}")

// ✅ CORRETO: Logs diretos, correlationId automaticamente incluído via MDC
logger.info("Executando [ação] de [entidade]: [campo]=${input.[campo]}")
logger.info("[Ação] de [entidade] concluída: id=${savedEntity.externalId}")

// ❌ ERRADO: Não incluir correlationId manualmente nos logs
logger.info("Executando [ação]: correlationId=${input.correlationId}")
```

### Extração de Métodos
- **Nomes descritivos**: `fetchExternalData()`, `validateBusinessRules()`, `createDomainEntity()`
- **Responsabilidade única**: Cada método faz uma coisa específica
- **Ordem lógica**: Métodos aparecem na ordem que são chamados

### Tratamento de CorrelationId
- **NUNCA repassar correlationId** para use cases ou DTOs
- **CorrelationIdFilter captura automaticamente** do header `x-correlation-id`
- **MDC inclui automaticamente** em todos os logs via SLF4J
- **Apenas documentar** no controller se necessário, não logar

### Integração com Domain Layer
- **Coleções mutáveis**: Entidades de domínio devem usar `MutableList` internamente e expor `List` publicamente
- **Enums dinâmicos**: Use `getAcceptedValues()` para mensagens de erro dinâmicas
- **Tratamento específico**: Capture `InvalidSessionEnumException` e converta para mensagem amigável

## ⚠️ PONTOS DE ATENÇÃO

### Use Cases
- **Orquestram, não decidem**: Regras ficam no domain
- **Método único execute()**: Command Pattern limpo e conciso
- **Transactional criterioso**: Apenas bancos relacionais
- **Logging sem correlationId**: CorrelationIdFilter inclui automaticamente via MDC
- **Sem dados sensíveis**: Mascaramento quando necessário

### DTOs
- **Sem valores default**: Explicitação sempre
- **Separação clara**: Input/Output vs Params/Result  
- **Sem correlationId**: Capturado automaticamente pelo CorrelationIdFilter
- **Mappers próximos**: Extension functions no arquivo

### Application Services
- **Interfaces**: Quando implementação está na infra
- **Classes concretas**: Lógica específica da aplicação
- **Não é domínio**: Validações entre agregados

### Config Providers
- **Apenas interfaces**: Implementação no application
- **Métodos específicos**: getTempo**Seconds**(), get**Minutes**()
- **Tipagem forte**: Boolean, Int, String específicos

### Tratamento de Erros
- **BusinessException**: Propaga sem modificar
- **InfrastructureException**: Loga detalhes, propaga
- **Exception genérica**: Encapsula em ApplicationException

---

### FEEDBACK
<!-- Registro de melhorias durante uso -->

### NOTAS DE VERSÃO

#### v1.1.0 - CLEAN CODE PATTERNS
- **Padrões Clean Code obrigatórios** para use cases
- **Organização de métodos**: execute() primeiro, métodos na ordem de execução
- **Imports limpos**: sem wildcards no código, imports específicos
- **Tratamento de exceções**: específico para enums inválidos
- **Integração domain**: coleções mutáveis e enums dinâmicos
- **Logs simplificados**: sem métodos auxiliares desnecessários

#### v1.0.0
- Versão inicial do APPLICATION-LAYER
- Implementação de use cases e DTOs internos
- Padrões para application services e config providers
