# 🔌 EXTERNAL-INTEGRATIONS - INTEGRAÇÕES EXTERNAS

---
id: external-integrations
version: 1.0.0
requires: [meta-prompt, project-context, initial-setup, infrastructure-base, domain-layer, application-layer]
provides: [feign-clients, aws-integrations, ldap-config, messaging-services, event-listeners]
optional: false
---

## 🎯 SEU PAPEL

Você irá implementar o módulo external que gerencia todas as integrações com sistemas externos seguindo Clean Architecture. Este módulo aplica o padrão Adapter (Hexagonal Architecture), isolando a aplicação de detalhes externos como APIs REST, LDAP, AWS services, mensageria e outros.

## 📋 PRÉ-REQUISITOS

Antes de iniciar:
1. **Interfaces de serviços** definidas no usecase
2. **Integrações mapeadas** nos fluxos
3. **URLs e configurações** dos sistemas externos
4. **Contratos de APIs** (request/response)

**IMPORTANTE**: Este prompt pode ser usado de forma independente. A IA deve:
1. Verificar se há contexto/integrações definidas
2. Se não houver, solicitar documentação das integrações
3. Trabalhar com o que for fornecido

## 🔄 FLUXO DE GERAÇÃO

### 1. Identificação das Integrações
```
"Vou verificar quais integrações externas precisam ser implementadas.

[VERIFICAR CONTEXTO DA CONVERSA]
- Procurando por interfaces de serviços no usecase...
- Procurando por integrações nos fluxos...
- Procurando por sistemas externos mencionados...

[SE ENCONTROU CONTEXTO]
Baseado no contexto, identifiquei estas integrações:

**APIs REST (Feign)**:
1. [Sistema]: [Nome]Service
   - Endpoints: [listar]
   - Autenticação: [tipo]

**AWS Services**:
1. Secrets Manager: [secrets identificados]
2. SQS: [filas identificadas]
3. SNS: [tópicos identificados]
4. S3: [buckets identificados]

**LDAP/AD**:
- Active Directory para [propósito]

**Mensageria**:
- Producers: [filas/tópicos]
- Listeners: [filas a consumir]

[SE NÃO ENCONTROU CONTEXTO]
Não encontrei integrações definidas.

Para implementar o módulo external, preciso que você forneça:

1. **APIs REST externas**:
   - Nome do sistema
   - Endpoints (método, path)
   - Request/Response
   - Autenticação

2. **AWS Services**:
   - SQS queues
   - SNS topics
   - S3 buckets
   - Secrets

3. **Outros sistemas**:
   - LDAP/AD
   - Mensageria
   - Webhooks

Qual integração gostaria de implementar primeiro?"
```

### 2. Organização por Tipo
```
"Vou organizar as integrações por tipo e contexto:

**Estrutura proposta**:
```
external/
├── config/
│   ├── aws/           → Configs AWS base
│   └── feign/         → Configs Feign base
├── [sistema-api]/     → Uma pasta por API REST
├── [contexto]/        → Agrupamento por domínio
│   ├── queue/         → SQS producers
│   └── topic/         → SNS publishers
└── eventlisteners/    → SQS/SNS consumers
```

**Princípios**:
- APIs REST: Um pacote por sistema
- Mensageria: Agrupada por contexto de negócio
- Listeners: Centralizados para fácil gestão
- Configs base: Reutilizáveis

Concorda com esta organização?"
```

### 3. Padrões de Implementação
```
"Para garantir consistência, vou aplicar estes padrões:

**APIs REST (Feign)**:
- FeignCallHelper para tratamento de erros
- ErrorDecoder com retry inteligente
- Request interceptors para logging

**AWS Services**:
- BaseSecretsManagerService como template
- Configurações unificadas (AwsMessagingConfig)
- LocalStack para desenvolvimento

**Tratamento de Erros**:
- Exceções específicas por componente
- InfrastructureException como base
- Logging estruturado

Confirma estes padrões?"
```

## 📁 ESTRUTURAS A SEREM GERADAS

### 1. ESTRUTURA BASE DO EXTERNAL

```
external/
└── src/main/kotlin/[package/path]/external/
    ├── config/
    │   ├── aws/
    │   │   ├── AwsBaseConfig.kt
    │   │   ├── AwsMessagingConfig.kt
    │   │   ├── BaseSecretsManagerService.kt
    │   │   └── S3Config.kt
    │   └── feign/
    │       ├── FeignBaseConfig.kt
    │       ├── FeignCallHelper.kt
    │       └── FeignErrorDecoder.kt
    ├── [sistema-externo]/         → Por sistema REST
    │   ├── client/
    │   │   └── [Sistema]FeignClient.kt
    │   ├── dto/
    │   │   ├── request/
    │   │   └── response/
    │   ├── exception/
    │   │   └── [Sistema]Exception.kt
    │   └── impl/
    │       └── [Sistema]ServiceImpl.kt
    ├── [contexto-negocio]/        → Por contexto
    │   ├── queue/
    │   │   ├── dto/
    │   │   └── impl/
    │   └── topic/
    │       ├── dto/
    │       └── impl/
    └── eventlisteners/            → Consumers centralizados
        └── [contexto]/
            ├── dto/
            └── handler/
```

### 2. CONFIGURAÇÕES BASE

#### AWS Base Config
```kotlin
package [package].external.config.aws

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region

@Configuration
class AwsBaseConfig(
    @Value("\${aws.region}") private val awsRegion: String,
    @Value("\${aws.local-stack.enable}") private val localStack: Boolean
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun awsCredentialsProvider(): AwsCredentialsProvider {
        logger.info("Configuring AWS credentials provider")
        return DefaultCredentialsProvider.create()
    }

    @Bean
    fun awsRegion(): Region {
        logger.info("AWS Region configured: $awsRegion")
        return Region.of(awsRegion)
    }

    @Bean
    fun isAwsLocalStack(): Boolean = localStack
}
```

#### Base Secrets Manager Service (Template Pattern)
```kotlin
package [package].external.config.aws

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException

abstract class BaseSecretsManagerService<T>(
    protected val objectMapper: ObjectMapper,
    protected val secretsManagerClient: SecretsManagerClient,
    protected val secretName: String,
    private val componentName: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    protected abstract fun parseSecret(secretString: String): T
    protected abstract fun validateSecret(secret: T)
    protected abstract fun createException(message: String, cause: Throwable?): RuntimeException

    @Cacheable(value = ["secrets"], key = "#root.target.secretName")
    fun getSecret(): T {
        return try {
            logger.debug("Fetching $componentName secret from AWS: secretName=$secretName")

            val request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build()

            val response = secretsManagerClient.getSecretValue(request)
            val secretString = response.secretString()

            if (secretString.isNullOrBlank()) {
                throw createException("Empty secret returned from AWS", null)
            }

            val secret = parseSecret(secretString)
            validateSecret(secret)

            logger.debug("$componentName secret obtained successfully")
            secret

        } catch (e: SecretsManagerException) {
            logger.error("AWS Secrets Manager error: ${e.message}", e)
            throw createException("AWS Secrets Manager error: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Unexpected error getting $componentName secret: ${e.message}", e)
            throw createException("Failed to get $componentName secret", e)
        }
    }
}
```

#### Feign Base Config
```kotlin
package [package].external.config.feign

import feign.Retryer
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignBaseConfig {

    @Bean
    fun feignRetryer(): Retryer {
        // 100ms initial, 1s max, 3 attempts
        return Retryer.Default(100, 1000, 3)
    }

    @Bean
    fun feignErrorDecoder(): ErrorDecoder {
        return FeignErrorDecoder()
    }
}
```

#### Feign Call Helper
```kotlin
package [package].external.config.feign

import [package].shared.exception.InfrastructureException
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FeignCallHelper {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun <T> executeFeignCall(
        componentName: String,
        operation: String,
        call: () -> T?,
        createException: (String, Throwable?) -> InfrastructureException,
        handle404AsNull: Boolean = false
    ): T? {
        logger.debug("Executing Feign call: component=$componentName, operation=$operation")

        return try {
            call()
        } catch (e: FeignException.NotFound) {
            if (handle404AsNull) {
                logger.debug("$componentName returned 404 - treating as null")
                null
            } else {
                throw createException("Resource not found", e)
            }
        } catch (e: FeignException) {
            logger.error("Error communicating with $componentName: status=${e.status()}, message=${e.message}")
            throw createException("Communication failed with $componentName", e)
        } catch (e: Exception) {
            logger.error("Unexpected error calling $componentName", e)
            throw createException("Unexpected error integrating with $componentName", e)
        }
    }
}
```

### 3. IMPLEMENTAÇÃO DE API REST (FEIGN)

#### Feign Client
```kotlin
package [package].external.[sistema].client

import [package].external.[sistema].dto.request.*
import [package].external.[sistema].dto.response.*
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(
    name = "[sistema-kebab]",
    url = "\${external-apis.[sistema-kebab].base-url}",
    configuration = [FeignBaseConfig::class]
)
interface [Sistema]FeignClient {

    @GetMapping("/api/v1/[recurso]/{id}")
    fun get[Recurso](
        @PathVariable id: String,
        [SE PRECISAR HEADERS]
        @RequestHeader("Authorization") token: String,
        @RequestHeader("x-correlation-id") correlationId: String
    ): [Recurso]Response?

    @PostMapping("/api/v1/[recurso]")
    fun create[Recurso](
        @RequestBody request: Create[Recurso]Request,
        [SE PRECISAR HEADERS]
        @RequestHeader("Authorization") token: String,
        @RequestHeader("x-correlation-id") correlationId: String
    ): Create[Recurso]Response

    [OUTROS ENDPOINTS]
}
```

#### Service Implementation
```kotlin
package [package].external.[sistema].impl

import [package].external.[sistema].client.[Sistema]FeignClient
import [package].external.[sistema].dto.request.*
import [package].external.[sistema].dto.response.*
import [package].external.[sistema].exception.[Sistema]Exception
import [package].external.config.feign.FeignCallHelper
import [package].usecase.[contexto].service.[Sistema]Service
import [package].usecase.[contexto].dto.params.*
import [package].usecase.[contexto].dto.result.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class [Sistema]ServiceImpl(
    private val feignClient: [Sistema]FeignClient,
    private val feignCallHelper: FeignCallHelper,
    [SE PRECISAR TOKEN]
    private val tokenProvider: [Sistema]TokenProvider
) : [Sistema]Service {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun get[Recurso](params: Get[Recurso]Params): Get[Recurso]Result? {
        logger.debug("Getting [recurso]: id=${params.id}")

        [SE PRECISAR TOKEN]
        val token = tokenProvider.getToken()

        val response = feignCallHelper.executeFeignCall(
            componentName = "[Sistema]",
            operation = "get[Recurso]",
            call = { feignClient.get[Recurso](params.id, token) },
            createException = { msg, cause -> [Sistema]Exception(msg, cause) },
            handle404AsNull = true // 404 returns null
        )

        return response?.toResult()
    }

    override fun create[Recurso](params: Create[Recurso]Params): Create[Recurso]Result {
        logger.debug("Creating [recurso]")

        val request = params.toRequest()

        val response = feignCallHelper.executeFeignCall(
            componentName = "[Sistema]",
            operation = "create[Recurso]",
            call = { feignClient.create[Recurso](request, token) },
            createException = { msg, cause -> [Sistema]Exception(msg, cause) },
            handle404AsNull = false
        ) ?: throw [Sistema]Exception("Null response from [Sistema]")

        return response.toResult()
    }
}
```

#### DTOs e Mappers
```kotlin
// Request DTO
package [package].external.[sistema].dto.request

data class Create[Recurso]Request(
    val [campo]: String,
    val [outrocampo]: Int
)

// Mapper from params
fun Create[Recurso]Params.toRequest(): Create[Recurso]Request {
    return Create[Recurso]Request(
        [campo] = this.[campo],
        [outrocampo] = this.[outrocampo]
    )
}

// Response DTO
package [package].external.[sistema].dto.response

data class [Recurso]Response(
    val id: String,
    val [campo]: String,
    val [status]: String
)

// Mapper to result
fun [Recurso]Response.toResult(): Get[Recurso]Result {
    return Get[Recurso]Result(
        id = this.id,
        [campo] = this.[campo],
        [status] = [Status]Enum.fromValue(this.[status])
    )
}
```

### 4. IMPLEMENTAÇÃO LDAP/AD

#### LDAP Config
```kotlin
package [package].external.ldap.config

import [package].external.ldap.service.LdapSecretsManagerService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.ldap.core.LdapTemplate
import org.springframework.ldap.core.support.LdapContextSource
import java.text.MessageFormat

@Configuration
@ConditionalOnProperty(
    value = ["properties.ldap.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class LdapConfig(
    private val ldapSecretsManagerService: LdapSecretsManagerService
) {

    @Bean
    fun ldapContextSource(): LdapContextSource {
        val secretConfig = ldapSecretsManagerService.getSecret()

        return LdapContextSource().apply {
            urls = secretConfig.ldapServerSsl.split(" ").toTypedArray()
            setBase(secretConfig.base)
            userDn = MessageFormat.format(secretConfig.userDn, secretConfig.username)
            password = secretConfig.password
            isPooled = true
            afterPropertiesSet()
        }
    }

    @Bean
    fun ldapTemplate(): LdapTemplate {
        return LdapTemplate(ldapContextSource()).apply {
            setIgnorePartialResultException(true)
            setIgnoreSizeLimitExceededException(true)
            setDefaultTimeLimit(30000)
            setDefaultCountLimit(1000)
            afterPropertiesSet()
        }
    }
}
```

### 5. IMPLEMENTAÇÃO MENSAGERIA (SQS/SNS)

#### AWS Messaging Config
```kotlin
package [package].external.config.aws

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

@Configuration
class AwsMessagingConfig(
    private val awsCredentialsProvider: AwsCredentialsProvider,
    private val awsRegion: Region,
    private val isAwsLocalStack: Boolean,
    @Value("\${aws.local-stack.endpoint}") private val localEndpoint: String
) {
    
    @Bean
    fun sqsClient(): SqsClient {
        val builder = SqsClient.builder()
            .region(awsRegion)
            .credentialsProvider(awsCredentialsProvider)
        
        if (isAwsLocalStack) {
            builder.endpointOverride(URI.create(localEndpoint))
        }
        
        return builder.build()
    }
    
    @Bean
    fun snsClient(): SnsClient {
        val builder = SnsClient.builder()
            .region(awsRegion)
            .credentialsProvider(awsCredentialsProvider)
        
        if (isAwsLocalStack) {
            builder.endpointOverride(URI.create(localEndpoint))
        }
        
        return builder.build()
    }
}
```

#### SQS Producer Implementation
```kotlin
package [package].external.[contexto].queue.impl

import [package].external.[contexto].queue.dto.[Mensagem]Message
import [package].external.[contexto].queue.exception.[Contexto]QueueException
import [package].usecase.[contexto].service.[Contexto]QueueService
import [package].usecase.[contexto].dto.params.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SqsException
import java.time.LocalDateTime

@Service
class [Contexto]QueueServiceImpl(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    @Value("\${aws.sqs.[nome-fila].url}") private val queueUrl: String
) : [Contexto]QueueService {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    override fun send[Mensagem](params: Send[Mensagem]Params) {
        logger.debug("Sending message to queue: [identificador]=${params.[identificador]}")
        
        try {
            val message = [Mensagem]Message(
                [campo] = params.[campo],
                timestamp = LocalDateTime.now()
            )
            
            val sendRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(objectMapper.writeValueAsString(message))
                [SE FOR FIFO]
                .messageGroupId("[grupo]")
                .build()
            
            val response = sqsClient.sendMessage(sendRequest)
            logger.info("Message sent successfully: messageId=${response.messageId()}")
            
        } catch (e: SqsException) {
            logger.error("Error sending to SQS: ${e.message}", e)
            throw [Contexto]QueueException("Failed to send message", e)
        }
    }
}
```

### 6. EVENT LISTENERS (CONSUMERS)

#### SQS Listener Config
```kotlin
package [package].external.eventlisteners.config

import io.awspring.cloud.sqs.config.SqsBootstrapConfiguration
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@Configuration
@Import(SqsBootstrapConfiguration::class)
class SqsListenerConfig {
    
    @Bean
    fun defaultSqsListenerContainerFactory(
        sqsAsyncClient: SqsAsyncClient
    ): SqsMessageListenerContainerFactory<Any> {
        return SqsMessageListenerContainerFactory
            .builder<Any>()
            .sqsAsyncClient(sqsAsyncClient)
            .build()
    }
}
```

#### Message Handler
```kotlin
package [package].external.eventlisteners.[contexto].handler

import [package].external.eventlisteners.[contexto].dto.[Evento]Message
import [package].usecase.[contexto].[Ação][Recurso]UseCase
import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class [Evento]MessageHandler(
    private val [ação][Recurso]UseCase: [Ação][Recurso]UseCase
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    @SqsListener(value = ["\${aws.sqs.[nome-fila].url}"])
    fun handleMessage(@Payload message: [Evento]Message) {
        logger.info("Message received: [id]=${message.[id]}")
        
        try {
            val input = message.toUseCaseInput()
            [ação][Recurso]UseCase.execute(input)
            logger.info("Message processed successfully")
        } catch (e: BusinessException) {
            logger.warn("Business error processing message: ${e.message}")
            // No retry for business errors
        } catch (e: Exception) {
            logger.error("Error processing message", e)
            throw e // SQS will retry
        }
    }
}
```

### 7. STORAGE (S3)

#### S3 Config
```kotlin
package [package].external.config.aws

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class S3Config(
    private val awsCredentialsProvider: AwsCredentialsProvider,
    private val awsRegion: Region,
    private val isAwsLocalStack: Boolean,
    @Value("\${aws.local-stack.endpoint}") private val localEndpoint: String
) {
    
    @Bean
    fun s3Client(): S3Client {
        val builder = S3Client.builder()
            .region(awsRegion)
            .credentialsProvider(awsCredentialsProvider)
        
        if (isAwsLocalStack) {
            builder.endpointOverride(URI.create(localEndpoint))
            builder.forcePathStyle(true)
        }
        
        return builder.build()
    }
}
```

#### S3 Service Implementation
```kotlin
package [package].external.[contexto].storage.impl

import [package].external.[contexto].storage.exception.[Contexto]StorageException
import [package].usecase.[contexto].service.[Contexto]StorageService
import [package].usecase.[contexto].dto.params.*
import [package].usecase.[contexto].dto.result.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*

@Service
class [Contexto]StorageServiceImpl(
    private val s3Client: S3Client,
    @Value("\${aws.s3.[bucket-name]}") private val bucketName: String
) : [Contexto]StorageService {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    override fun upload[Arquivo](params: Upload[Arquivo]Params): Upload[Arquivo]Result {
        val key = generateKey(params)
        logger.debug("Uploading file: key=$key")
        
        try {
            val metadata = mapOf(
                "[metadado]" to params.[metadado],
                "uploadedAt" to LocalDateTime.now().toString()
            )
            
            val putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(params.contentType)
                .metadata(metadata)
                .build()
            
            val response = s3Client.putObject(
                putRequest, 
                RequestBody.fromBytes(params.content)
            )
            
            logger.info("File uploaded successfully: eTag=${response.eTag()}")
            
            return Upload[Arquivo]Result(
                key = key,
                eTag = response.eTag()
            )
            
        } catch (e: S3Exception) {
            logger.error("Error uploading to S3: ${e.message}", e)
            throw [Contexto]StorageException("Failed to upload file", e)
        }
    }
    
    override fun download[Arquivo](params: Download[Arquivo]Params): ByteArray? {
        logger.debug("Downloading file: key=${params.key}")
        
        return try {
            val getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(params.key)
                .build()
            
            val response = s3Client.getObject(getRequest)
            response.readAllBytes()
            
        } catch (e: NoSuchKeyException) {
            logger.debug("File not found: key=${params.key}")
            null // 404 pattern
        } catch (e: S3Exception) {
            logger.error("Error downloading from S3: ${e.message}", e)
            throw [Contexto]StorageException("Failed to download file", e)
        }
    }
    
    private fun generateKey(params: Upload[Arquivo]Params): String {
        return "[prefixo]/${params.[contexto]}/${params.[id]}.${params.extension}"
    }
}
```

### 8. EXCEÇÕES ESPECÍFICAS

```kotlin
// Por sistema externo
package [package].external.[sistema].exception

import [package].shared.exception.InfrastructureException

class [Sistema]Exception(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "[Sistema]",
    message = message,
    cause = cause
)

// Por contexto de mensageria
package [package].external.[contexto].queue.exception

class [Contexto]QueueException(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "[Contexto]Queue",
    message = message,
    cause = cause
)

// Por contexto de storage
package [package].external.[contexto].storage.exception

class [Contexto]StorageException(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "[Contexto]Storage",
    message = message,
    cause = cause
)
```

## 🎯 PROCESSO DE GERAÇÃO

1. **Verifique contexto existente** ou solicite documentação
2. **Identifique tipos de integração** (REST, AWS, LDAP, etc)
3. **Organize por padrão**:
   - APIs REST: Por sistema
   - Mensageria/Storage: Por contexto
   - Listeners: Centralizados
4. **Implemente ordem**:
   - Configurações base
   - Helpers reutilizáveis
   - Implementações específicas
   - Exceções por componente
5. **Use artifacts separados** por tipo de integração
6. **Mantenha consistência** nos padrões

## ⚠️ PONTOS DE ATENÇÃO

### Isolamento de Responsabilidades
- **Sem lógica de negócio**: Apenas adaptação
- **Confia no usecase**: Sem revalidações
- **Converte formatos**: Externo ↔ Interno
- **Trata erros técnicos**: Não de negócio

### APIs REST (Feign)
- **FeignCallHelper sempre**: Centraliza tratamento
- **404 como null**: Quando faz sentido
- **Retry inteligente**: Apenas idempotentes
- **DTOs específicos**: Por sistema externo

### AWS Services
- **LocalStack**: Para desenvolvimento
- **Configs unificadas**: Por tipo (Messaging, S3)
- **Template pattern**: Para Secrets Manager
- **Exceções específicas**: Por componente

### Mensageria
- **Organização por contexto**: Não por tecnologia
- **Producers vs Consumers**: Separados
- **No retry em business errors**: Apenas infra
- **Listeners centralizados**: Fácil gestão

### Storage
- **NoSuchKeyException**: Tratado como null
- **Metadata**: Para rastreabilidade
- **Keys estruturadas**: Com prefixos contextuais

---

### FEEDBACK
<!-- Registro de melhorias durante uso -->

### NOTAS DE VERSÃO

#### v1.0.0
- Versão inicial do EXTERNAL-INTEGRATIONS
- Clientes Feign para integrações com APIs externas
- Configurações AWS, tratamento de erros e retry policies
