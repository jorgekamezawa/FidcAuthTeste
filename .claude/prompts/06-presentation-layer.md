# 🌐 PRESENTATION-LAYER - CAMADA WEB (APIs REST)

---
id: presentation-layer
version: 2.0.0
requires: [meta-prompt, project-context, initial-setup, infrastructure-base, domain-layer, application-layer]
provides: [rest-apis, swagger-docs, exception-handling, request-validation]
optional: false
---

## 🎯 SEU PAPEL

Você irá implementar a camada Presentation (módulo web) responsável por expor APIs REST, documentar endpoints, validar requisições e converter entre DTOs da web (Request/Response) e da aplicação (Input/Output). Esta camada segue o padrão de Controllers finos sem lógica de negócio.

**IMPORTANTE**: CorrelationId é capturado automaticamente pelo `CorrelationIdFilter` e incluído em todos os logs via MDC. NUNCA repassar para use cases.

## 📋 PRÉ-REQUISITOS

Antes de iniciar:
1. **Use cases implementados** na camada Application
2. **DTOs Input/Output** definidos no usecase
3. **Exceções customizadas** definidas no shared
4. **Fluxos mapeados** com contratos de API

**IMPORTANTE**: Este prompt pode ser usado de forma independente. A IA deve:
1. Verificar se há contexto/fluxos definidos anteriormente
2. Se não houver, solicitar documentação dos endpoints
3. Trabalhar com o que for fornecido

## 🔄 FLUXO DE GERAÇÃO

### 1. Identificação dos Endpoints e Contexto
```
"Vou verificar os endpoints REST a serem implementados.

[VERIFICAR CONTEXTO DA CONVERSA]
- Procurando por fluxos mapeados no PROJECT-CONTEXT...
- Procurando por use cases já implementados...
- Procurando por documentação de APIs...

[SE ENCONTROU CONTEXTO/FLUXOS]
Baseado no contexto, identifiquei estes endpoints:

1. **[MÉTODO] /[caminho]**
   - Controller: [Nome]Controller
   - Use Case: [Nome]UseCase
   - Descrição: [o que faz]

2. **[MÉTODO] /[caminho]**
   - Controller: [Nome]Controller
   - Use Case: [Nome]UseCase
   - Descrição: [o que faz]

[SE NÃO ENCONTROU CONTEXTO/FLUXOS]
Não encontrei contexto ou endpoints mapeados.

Para implementar a camada web, preciso que você forneça:

1. **Endpoints a implementar**:
   - Método HTTP e caminho
   - Headers obrigatórios/opcionais
   - Request body (se houver)
   - Response esperado
   - Códigos de erro possíveis

2. **Use cases correspondentes**:
   - Nome do use case
   - Input/Output DTOs

Exemplo:
```
POST /api/v1/auth/send-token
Headers: partner (obrigatório), x-correlation-id (opcional)
Request: { "signedData": "jwt-token" }
Response: { "userEmail": "j***@email.com", "cooldownSeconds": 30 }
Use Case: SendTokenMfaUseCase
```

Qual endpoint gostaria de implementar primeiro?"
```

### 2. Estrutura dos Controllers
```
"Para cada contexto de negócio, vou criar:

📁 **Estrutura por contexto**:
- [contexto]/controller/[Nome]Controller.kt
- [contexto]/documentation/[Nome]ApiDoc.kt
- [contexto]/dto/request/[Ação][Recurso]Request.kt
- [contexto]/dto/response/[Ação][Recurso]Response.kt

🎯 **Padrão de implementação**:
- Controllers implementam interfaces de documentação
- Swagger separado da lógica
- Validação com Bean Validation
- Mappers como extension functions

Confirma esta estrutura?"
```

### 3. Tratamento de Exceções Estruturado
```
"Para tratamento de erros, vou implementar handlers estruturados por contexto:

**Handler Global** (Order 100):
- Captura exceções não tratadas (Exception.class)
- Erros de validação Bean Validation (MethodArgumentNotValidException) 
- Headers obrigatórios ausentes (MissingRequestHeaderException)
- IllegalArgumentException para validações customizadas

**Handler Específico por Contexto** (Order 1):
- Métodos @ExceptionHandler agrupados por tipo de erro e comportamento:
  * Grupo 1: Validação (400) - InvalidInputException, ValidationException
  * Grupo 2: Recursos não encontrados (404) - NotFoundException
  * Grupo 3: Serviços indisponíveis (503) - IntegrationException
  * Grupo 4: Erros internos (500) - ProcessingException
  * Grupo 5: Infraestrutura (500/503) - InfrastructureException

**ANTI-PADRÕES A EVITAR**:
- ❌ NUNCA usar when statements com message parsing
- ❌ NUNCA analisar ex.message para determinar status HTTP
- ❌ NUNCA usar lógica complexa de decisão baseada em strings

**PADRÕES OBRIGATÓRIOS**:
- ✅ Um @ExceptionHandler por grupo de exceções similares
- ✅ Logging estruturado com contexto da requisição
- ✅ Mensagens padronizadas em português para erros 500
- ✅ Usar ex.message diretamente para erros de validação (400/404)

**Formatos de erro padrão**:

*ErrorResponse (400, 403, 404, 500, 503):*
```json
{
  "timestamp": "2025-08-28T14:45:32",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensagem específica do erro",
  "path": "/v1/sessions"
}
```

*ValidationErrorResponse (400 - Bean Validation):*
```json
{
  "timestamp": "2025-08-28T14:45:32",
  "status": 400,
  "error": "Bad Request",
  "message": "Dados de entrada inválidos",
  "path": "/v1/sessions",
  "errors": {
    "signedData": "signedData é obrigatório"
  }
}
```

Concorda com esta estratégia estruturada?"
```

## 📁 ESTRUTURAS A SEREM GERADAS

### 1. ESTRUTURA DO WEB

```
web/
└── src/main/kotlin/[package/path]/web/
    ├── common/
    │   ├── config/
    │   │   └── SwaggerConfig.kt
    │   ├── exception/
    │   │   ├── dto/
    │   │   │   ├── ErrorResponse.kt
    │   │   │   └── ValidationErrorResponse.kt
    │   │   └── handler/
    │   │       └── GlobalExceptionHandler.kt
    │   └── filter/
    │       └── CorrelationIdFilter.kt
    └── [contexto]/
        ├── controller/
        │   └── [Nome]Controller.kt
        ├── documentation/
        │   └── [Nome]ApiDoc.kt
        ├── dto/
        │   ├── request/
        │   │   └── [Ação][Recurso]Request.kt
        │   └── response/
        │       └── [Ação][Recurso]Response.kt
        └── exception/
            └── [Contexto]ExceptionHandler.kt
```

### 2. CONFIGURAÇÕES COMUNS

#### SwaggerConfig.kt
```kotlin
package [package].web.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Value("\${spring.application.name}")
    private lateinit var applicationName: String

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("$applicationName API")
                    .description("[Descrição do projeto baseada no contexto]")
                    .version("1.0.0")
            )
            .servers(
                listOf(
                    Server()
                        .url("/")
                        .description("Current server")
                )
            )
    }
}
```

#### CorrelationIdFilter.kt (JÁ EXISTE NO INITIAL-SETUP)
```kotlin
// Este arquivo já foi criado no INITIAL-SETUP no módulo web
// Apenas referenciar que já existe
```

#### ErrorResponse DTOs (Português)
```kotlin
package [package].web.common.exception.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Resposta padrão de erro da API")
data class ErrorResponse(
    @Schema(description = "Timestamp do erro", example = "2025-08-28T14:45:32")
    val timestamp: LocalDateTime,
    
    @Schema(description = "Código de status HTTP", example = "400")
    val status: Int,
    
    @Schema(description = "Tipo do erro", example = "Bad Request")
    val error: String,
    
    @Schema(description = "Mensagem descritiva do erro", example = "Dados inválidos")
    val message: String,
    
    @Schema(description = "Caminho da requisição que causou o erro", example = "/v1/sessions")
    val path: String
)

@Schema(description = "Resposta de erro de validação com detalhes por campo")
data class ValidationErrorResponse(
    @Schema(description = "Timestamp do erro", example = "2025-08-28T14:45:32")
    val timestamp: LocalDateTime,
    
    @Schema(description = "Código de status HTTP", example = "400")
    val status: Int,
    
    @Schema(description = "Tipo do erro", example = "Bad Request")
    val error: String,
    
    @Schema(description = "Mensagem geral do erro", example = "Dados de entrada inválidos")
    val message: String,
    
    @Schema(description = "Caminho da requisição que causou o erro", example = "/v1/sessions")
    val path: String,
    
    @Schema(description = "Erros específicos por campo", example = "{\"signedData\": \"signedData é obrigatório\"}")
    val errors: Map<String, String>
)
```

#### GlobalExceptionHandler.kt
```kotlin
package [package].web.common.exception.handler

import [package].web.common.exception.dto.ErrorResponse
import [package].web.common.exception.dto.ValidationErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
@Order(100) // Execute last
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingRequestHeader(
        ex: MissingRequestHeaderException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val message = "Required header '${ex.headerName}' is missing"
        logger.warn("Missing header: {}", ex.headerName)
        
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ValidationErrorResponse> {
        logger.warn("Validation error: {} errors found", ex.bindingResult.fieldErrorCount)

        val fieldErrors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "Invalid value")
        }

        val errorResponse = ValidationErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Validation error in request fields",
            path = request.requestURI,
            errors = fieldErrors
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid argument: {}", ex.message)
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid argument", request)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled error: {}", ex.message, ex)
        val message = "Erro interno do sistema. Entre em contato com o suporte técnico se o problema persistir."
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message, request)
    }

    private fun buildErrorResponse(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(errorResponse)
    }
}
```

### 3. IMPLEMENTAÇÃO POR CONTEXTO

#### API Documentation Interface
```kotlin
package [package].web.[contexto].documentation

import [package].web.[contexto].dto.request.[Ação][Recurso]Request
import [package].web.[contexto].dto.response.[Ação][Recurso]Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@Tag(
    name = "[Contexto de Negócio em Português]",
    description = "[Descrição completa das operações deste contexto em português]"
)
interface [Nome]ApiDoc {

    @Operation(
        summary = "[Resumo da operação em português]",
        description = "[Descrição detalhada do que a operação faz, incluindo validações e comportamentos específicos]"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "[Descrição do sucesso em português]",
            content = [Content(
                schema = Schema(implementation = [Ação][Recurso]Response::class),
                examples = [ExampleObject(
                    name = "Sucesso",
                    value = """{"campo": "valor", "outrocampo": 123}"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "4XX",
            description = "Erros de validação de campos - Retorna ValidationErrorResponse",
            content = [Content(
                schema = Schema(implementation = ValidationErrorResponse::class),
                examples = [ExampleObject(
                    name = "ValidationErrorResponse",
                    value = """{
                        "timestamp": "2025-08-28T14:45:32",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Dados de entrada inválidos",
                        "path": "/v1/[recurso]",
                        "errors": {
                            "campo": "campo é obrigatório"
                        }
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "default",
            description = "Erros gerais - Retorna ErrorResponse para códigos 403, 404, 503, etc.",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "ErrorResponse",
                    value = """{
                        "timestamp": "2025-08-28T14:45:32",
                        "status": 403,
                        "error": "Forbidden",
                        "message": "Usuário não autorizado",
                        "path": "/v1/[recurso]"
                    }"""
                )]
            )]
        )
    ])
    fun [ação][Recurso](
        @RequestBody request: [Ação][Recurso]Request,
        
        [SE TIVER HEADERS OBRIGATÓRIOS]
        @Parameter(description = "[Descrição do header]", required = true)
        @RequestHeader("[header-name]") [headerName]: String,
        
        [SE TIVER USER-AGENT (SEMPRE OBRIGATÓRIO)]
        @Parameter(description = "User agent string from client", required = true)
        @RequestHeader("user-agent") userAgent: String,
        
        [SE TIVER HEADERS OPCIONAIS PARA DOCUMENTAÇÃO]
        @Parameter(description = "Tracking correlation ID (managed by CorrelationIdFilter)", required = false)
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,
        
        [SE PRECISAR DE IP DO CLIENTE]
        @Parameter(hidden = true) httpRequest: HttpServletRequest
    ): [Ação][Recurso]Response
}
```

#### Controller Implementation
```kotlin
package [package].web.[contexto].controller

import [package].usecase.[contexto].[Ação][Recurso]UseCase
import [package].web.[contexto].documentation.[Nome]ApiDoc
import [package].web.[contexto].dto.request.*
import [package].web.[contexto].dto.response.*
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/[recurso]")
class [Nome]Controller(
    private val [ação][Recurso]UseCase: [Ação][Recurso]UseCase
) : [Nome]ApiDoc {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/[ação]")
    @ResponseStatus(HttpStatus.OK)
    override fun [ação][Recurso](
        @Valid @RequestBody request: [Ação][Recurso]Request,
        [SE TIVER HEADERS OBRIGATÓRIOS]
        @RequestHeader("[header-name]") [headerName]: String,
        [SE TIVER USER-AGENT]
        @RequestHeader("user-agent") userAgent: String,
        [SE TIVER CORRELATION ID APENAS PARA DOCUMENTAÇÃO]
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,
        [SE PRECISAR IP]httpRequest: HttpServletRequest
    ): [Ação][Recurso]Response {
        logger.info("Received [ação] request: [headerName]=${[headerName]}")

        // Validação de headers obrigatórios - Spring não valida strings vazias
        require([headerName].isNotBlank()) { "Header '[header-name]' cannot be empty" }
        require(userAgent.isNotBlank()) { "Header 'user-agent' cannot be empty" }

        [SE PRECISAR IP DO CLIENTE]
        val clientIpAddress = httpRequest.getClientIp()
        
        val input = request.toInput(
            [headerName] = [headerName],
            userAgent = userAgent,
            [SE PRECISAR IP]clientIpAddress = clientIpAddress
            // NUNCA incluir correlationId - gerenciado pelo CorrelationIdFilter
        )
        val output = [ação][Recurso]UseCase.execute(input)
        val response = output.toResponse()

        logger.info("[Ação] completed successfully")
        return response
    }
}
```

#### Request DTOs
```kotlin
package [package].web.[contexto].dto.request

import [package].usecase.[contexto].dto.input.[Ação][Recurso]Input
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.*

@Schema(description = "[Descrição completa do request em português]")
data class [Ação][Recurso]Request(
    [PARA CAMPOS OBRIGATÓRIOS]
    @field:NotBlank(message = "[Campo] is required")
    @field:Size(min = [min], max = [max], message = "[Campo] must be between [min] and [max] characters")
    @Schema(
        description = "[Descrição do campo]",
        example = "[exemplo]",
        required = true
    )
    val [campo]: String,
    
    [PARA CAMPOS OPCIONAIS]
    @field:Size(max = [max], message = "[Campo] must not exceed [max] characters")
    @Schema(
        description = "[Descrição do campo]",
        example = "[exemplo]",
        required = false
    )
    val [campoOpcional]: String? = null,
    
    [PARA LISTAS]
    @field:NotEmpty(message = "[Campo] cannot be empty")
    @field:Size(max = [max], message = "Maximum [max] items allowed")
    @Schema(
        description = "[Descrição]",
        required = true
    )
    val [items]: List<[Tipo]>,
    
    [PARA NÚMEROS]
    @field:Min(value = [min], message = "[Campo] must be at least [min]")
    @field:Max(value = [max], message = "[Campo] must not exceed [max]")
    @Schema(
        description = "[Descrição]",
        example = "[exemplo]",
        required = true
    )
    val [numero]: Int
)

// Mapper as extension function - NUNCA incluir correlationId
fun [Ação][Recurso]Request.toInput(
    [headerParam]: String,
    userAgent: String,
    [SE PRECISAR]clientIpAddress: String
): [Ação][Recurso]Input {
    return [Ação][Recurso]Input(
        [campo] = this.[campo],
        [campoOpcional] = this.[campoOpcional],
        [headerParam] = [headerParam],
        userAgent = userAgent,
        [SE PRECISAR]clientIpAddress = clientIpAddress
        // NUNCA incluir correlationId - gerenciado automaticamente
    )
}
```

#### Response DTOs
```kotlin
package [package].web.[contexto].dto.response

import [package].usecase.[contexto].dto.output.[Ação][Recurso]Output
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Schema(description = "[Descrição completa do response em português]")
data class [Ação][Recurso]Response(
    @Schema(description = "[Descrição]", example = "[exemplo]")
    val [campo]: String,
    
    @Schema(description = "[Descrição]", example = "[exemplo numérico]")
    val [numero]: Int,
    
    [SE TIVER CAMPO SENSÍVEL MASCARADO]
    @Schema(description = "Masked email", example = "j***@example.com")
    val userEmail: String,
    
    [SE TIVER DATA FORMATADA]
    @Schema(description = "Creation date", example = "2025-07-24T14:45:32")
    val createdAt: String
)

// Response pode formatar dados para apresentação
fun [Ação][Recurso]Output.toResponse(): [Ação][Recurso]Response {
    return [Ação][Recurso]Response(
        [campo] = this.[campo],
        [numero] = this.[numero],
        [SE TIVER FORMATAÇÃO DE DATA]
        createdAt = this.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        [SE TIVER MASCARAMENTO]
        userEmail = maskEmail(this.userEmail)
    )
}

[SE PRECISAR FUNÇÕES AUXILIARES]
private fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return "***@***"
    
    val name = parts[0]
    val domain = parts[1]
    
    return if (name.length > 2) {
        "${name.first()}${"*".repeat(name.length - 2)}${name.last()}@$domain"
    } else {
        "${"*".repeat(name.length)}@$domain"
    }
}
```

#### Context-Specific Exception Handler (Estruturado por Grupos)
```kotlin
package [package].web.[contexto].exception

import [package].shared.exception.*
import [package].usecase.[contexto].exception.*
import [package].web.common.exception.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
@Order(1) // Execute first, before global handler
class [Contexto]ExceptionHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // Grupo 1: Validação de Entrada (400) - Usar ex.message
    @ExceptionHandler([Contexto]ValidationException::class, InvalidInputException::class)
    fun handleValidationErrors(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("[Contexto] validation error on {}: {}", request.requestURI, ex.message)
        
        val message = ex.message ?: "Invalid request data"
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    // Grupo 2: Recursos Não Encontrados (404) - Usar ex.message  
    @ExceptionHandler([Contexto]NotFoundException::class)
    fun handleNotFoundErrors(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("[Contexto] not found on {}: {}", request.requestURI, ex.message)
        
        val message = ex.message ?: "Resource not found"
        return buildErrorResponse(HttpStatus.NOT_FOUND, message, request)
    }

    // Grupo 3: Serviços Indisponíveis (503) - Mensagem customizada
    @ExceptionHandler([External]IntegrationException::class)
    fun handleServiceUnavailableErrors(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Service integration failure on {}: {}", request.requestURI, ex.message, ex)
        
        val message = when (ex) {
            is UserManagementIntegrationException -> "Serviço de usuários temporariamente indisponível"
            is [Other]IntegrationException -> "Serviço [nome] temporariamente indisponível"
            else -> "Serviço temporariamente indisponível"
        }
        
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, message, request)
    }

    // Grupo 4: Erros Internos (500) - Mensagem padronizada + logging completo
    @ExceptionHandler([Contexto]ProcessingException::class)
    fun handleInternalProcessingErrors(
        ex: [Contexto]ProcessingException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error(
            "[Contexto] processing failure on {} - Request: {} - Error: {}", 
            request.requestURI,
            request.queryString ?: "no-query",
            ex.message,
            ex
        )
        
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Ocorreu um erro interno no sistema. Entre em contato com o suporte técnico se o problema persistir.",
            request
        )
    }

    // Grupo 5: Infraestrutura (500/503) - Sempre erros técnicos
    @ExceptionHandler(InfrastructureException::class)
    fun handleInfrastructureErrors(
        ex: InfrastructureException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error(
            "Infrastructure failure [{}] on {} - Request: {} - Error: {}", 
            ex.component,
            request.requestURI,
            request.queryString ?: "no-query",
            ex.message,
            ex
        )

        val (status, message) = when (ex.component) {
            "Redis", "RedisRepository", "PostgreSQL", "[Component]Repository" -> 
                HttpStatus.SERVICE_UNAVAILABLE to "Serviço temporariamente indisponível"
            else -> 
                HttpStatus.INTERNAL_SERVER_ERROR to "Ocorreu um erro interno no sistema. Entre em contato com o suporte técnico se o problema persistir."
        }

        return buildErrorResponse(status, message, request)
    }

    private fun buildErrorResponse(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(errorResponse)
    }
}
```

## 🎯 PROCESSO DE GERAÇÃO

1. **Verifique contexto existente** ou solicite documentação de endpoints
2. **Identifique controllers** baseados nos use cases
3. **Confirme estrutura** por contexto de negócio
4. **Implemente ordem**:
   - Configurações comuns (Swagger, DTOs de erro)
   - Exception handlers (global e específicos)
   - Por contexto: Documentation → DTOs → Controller
5. **Use artifacts separados** por contexto
6. **Mantenha padrões**: 
   - Controllers finos
   - Documentação separada
   - Mappers como extension functions

## 🚫 REGRAS OBRIGATÓRIAS DO CORRELATIONID

### CorrelationId Management
- **NUNCA repassar correlationId** para use cases ou DTOs de input
- **CorrelationIdFilter captura automaticamente** do header `x-correlation-id`
- **MDC inclui automaticamente** em todos os logs via SLF4J
- **Manter header apenas para documentação** da API (não usar no código)
- **Logs automaticamente incluem correlationId** sem intervenção manual

### Headers Obrigatórios
- **user-agent**: SEMPRE obrigatório e mapeado como `@RequestHeader`
- **NUNCA pegar headers do HttpServletRequest** exceto para IP do cliente
- **HttpServletRequest**: Usar apenas para `getClientIp()` quando necessário

## ⚠️ PONTOS DE ATENÇÃO

### Controllers
- **Thin controllers**: Sem lógica de negócio
- **Implementam interfaces**: Documentação separada
- **Validação adicional**: Headers vazios (Spring não valida)
- **Logging limpo**: Sem correlationId manual (automático via MDC)

### DTOs
- **Request/Response**: Diferentes de Input/Output
- **Validação completa**: Bean Validation annotations
- **Mappers próximos**: Extension functions no arquivo
- **Formatação**: Response pode formatar para apresentação

### Exception Handling
- **Order importante**: 1 para específicos, 100 para global
- **503 para infra**: Indica problema temporário
- **Mensagens genéricas**: Não expor detalhes internos
- **Logging diferenciado**: WARN para negócio, ERROR para infra

### Swagger
- **Exemplos realísticos**: Para cada response code principal
- **Descrições claras**: EM PORTUGUÊS (mudou de inglês para português)
- **Simplificação de erros**: Máximo 2 exemplos (ValidationErrorResponse + ErrorResponse)
- **Padrão de responseCode**: '4XX' para validação, 'default' para erros gerais
- **Pattern informativo**: Validação real no backend
- **Default values**: JWT tokens e valores de exemplo para facilitar testes
- **Tags organizadas**: Por contexto de negócio

## 📋 CHECKLIST DE IMPLEMENTAÇÃO

### Antes de Iniciar
- [ ] Verificar use cases implementados na camada Application
- [ ] Confirmar DTOs Input/Output definidos no usecase  
- [ ] Validar exceções customizadas definidas no shared
- [ ] Mapear endpoints e contratos de API

### Exception Handlers (OBRIGATÓRIO)
- [ ] **NUNCA usar when statements** com message parsing
- [ ] **NUNCA analisar ex.message** para determinar status HTTP
- [ ] Implementar handlers agrupados por comportamento:
  - [ ] Grupo 1: Validação (400) - InvalidInputException, ValidationException
  - [ ] Grupo 2: Not Found (404) - NotFoundException  
  - [ ] Grupo 3: Service Unavailable (503) - IntegrationException
  - [ ] Grupo 4: Internal Error (500) - ProcessingException
  - [ ] Grupo 5: Infrastructure (500/503) - InfrastructureException
- [ ] Logging estruturado com request.requestURI e queryString
- [ ] Mensagens padronizadas em português para erros 500
- [ ] Usar ex.message diretamente para erros de validação

### Swagger Documentation
- [ ] Todas as descrições em **PORTUGUÊS**
- [ ] Máximo 2 exemplos de erro por endpoint:
  - [ ] "4XX" → ValidationErrorResponse
  - [ ] "default" → ErrorResponse  
- [ ] Default values em @Parameter para facilitar testes
- [ ] JWT tokens de exemplo fornecidos pelo usuário
- [ ] Tags organizadas por contexto de negócio

### Padrões da Empresa
- **Correlation ID**: Gerenciado pelo CorrelationIdFilter, NUNCA repassar manualmente
- **User-Agent**: Sempre obrigatório via @RequestHeader
- **No JsonProperty**: Apenas quando nome diferente
- **Mínimo comentários**: Código auto-explicativo
- **Headers lowercase**: Padrão HTTP

---

### FEEDBACK
<!-- Registro de melhorias durante uso -->

### NOTAS DE VERSÃO

#### v2.0.0
- **Exception Handling Estruturado**: Eliminação de when statements e message parsing
- **Handlers Agrupados**: Métodos @ExceptionHandler por tipo de comportamento
- **Swagger em Português**: Toda documentação traduzida para português
- **Simplificação de Exemplos**: Máximo 2 exemplos por endpoint (4XX + default)
- **Padrão ResponseCode**: '4XX' para ValidationErrorResponse, 'default' para ErrorResponse
- **Mensagens Padronizadas**: Erros 500 em português, validação usa ex.message
- **Logging Estruturado**: Com contexto completo da requisição
- **Default Values**: JWT tokens e exemplos prontos para facilitar testes da API

#### v1.0.0
- Versão inicial do PRESENTATION-LAYER
- Controllers REST com documentação Swagger automatizada
- Exception handlers globais e validação de requests
