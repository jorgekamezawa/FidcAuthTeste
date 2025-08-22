# 🌐 PRESENTATION-LAYER - CAMADA WEB (APIs REST)

---
id: presentation-layer
version: 1.0.0
requires: [meta-prompt, project-context, initial-setup, infrastructure-base, domain-layer, application-layer]
provides: [rest-apis, swagger-docs, exception-handling, request-validation]
optional: false
---

## 🎯 SEU PAPEL

Você irá implementar a camada Presentation (módulo web) responsável por expor APIs REST, documentar endpoints, validar requisições e converter entre DTOs da web (Request/Response) e da aplicação (Input/Output). Esta camada segue o padrão de Controllers finos sem lógica de negócio.

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

### 3. Tratamento de Exceções
```
"Para tratamento de erros, vou implementar:

**Handler Global** (Order 100):
- Captura exceções não tratadas
- Erros de validação
- Headers obrigatórios ausentes

**Handler Específico** (Order 1):
- BusinessException → 400/403/404
- InfrastructureException → 503
- Exceções específicas do contexto

**Formato de erro padrão**:
```json
{
  "timestamp": "2025-07-24T14:45:32",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensagem específica",
  "path": "/api/endpoint"
}
```

Concorda com esta estratégia?"
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

#### ErrorResponse DTOs
```kotlin
package [package].web.common.exception.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Standard API error response")
data class ErrorResponse(
    @Schema(description = "Error timestamp", example = "2025-06-30T10:30:00")
    val timestamp: LocalDateTime,
    
    @Schema(description = "HTTP status code", example = "400")
    val status: Int,
    
    @Schema(description = "Error type", example = "Bad Request")
    val error: String,
    
    @Schema(description = "Error message", example = "Invalid data")
    val message: String,
    
    @Schema(description = "Request path", example = "/api/v1/resource")
    val path: String
)

@Schema(description = "Validation error response with field details")
data class ValidationErrorResponse(
    @Schema(description = "Error timestamp", example = "2025-06-30T10:30:00")
    val timestamp: LocalDateTime,
    
    @Schema(description = "HTTP status code", example = "400")
    val status: Int,
    
    @Schema(description = "Error type", example = "Bad Request")
    val error: String,
    
    @Schema(description = "General message", example = "Invalid input data")
    val message: String,
    
    @Schema(description = "Field-specific errors")
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
        val message = "Internal system error. Please contact support."
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
    name = "[Contexto de Negócio]",
    description = "[Descrição das operações deste contexto]"
)
interface [Nome]ApiDoc {

    @Operation(
        summary = "[Resumo da operação]",
        description = "[Descrição detalhada do que a operação faz]"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Success response",
            content = [Content(
                schema = Schema(implementation = [Ação][Recurso]Response::class),
                examples = [ExampleObject(
                    name = "Success",
                    value = """{"campo": "valor", "outrocampo": 123}"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad Request - Invalid input data",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    value = """{
                        "timestamp": "2025-07-24T14:45:32",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Invalid data provided",
                        "path": "/api/v1/[recurso]"
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "403",
            description = "Forbidden - [Specific case]",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found - [Resource] not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "503",
            description = "Service Unavailable - External service temporarily unavailable",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun [ação][Recurso](
        @RequestBody request: [Ação][Recurso]Request,
        
        [SE TIVER HEADERS OBRIGATÓRIOS]
        @Parameter(description = "[Descrição do header]", required = true)
        @RequestHeader("[header-name]") [headerName]: String,
        
        [SE TIVER HEADERS OPCIONAIS]
        @Parameter(description = "Tracking ID", required = false)
        @RequestHeader("x-correlation-id", required = false) correlationId: String?
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
        [SE TIVER HEADERS OPCIONAIS]
        @RequestHeader("x-correlation-id", required = false) correlationId: String?
    ): [Ação][Recurso]Response {
        logger.info(
            "Received [ação] request: [headerName]=${[headerName]}, correlationId=${correlationId ?: "not-provided"}"
        )

        [SE TIVER HEADERS OBRIGATÓRIOS]
        // Additional validation - Spring doesn't validate empty strings
        require([headerName].isNotBlank()) { "Header '[header-name]' cannot be empty" }

        val input = request.toInput([SE HEADER FAZ PARTE DO INPUT][headerName])
        val output = [ação][Recurso]UseCase.execute(input)
        val response = output.toResponse()

        logger.info("[Ação] completed successfully: [log relevante sem dados sensíveis]")
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

@Schema(description = "[Descrição do request]")
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

// Mapper as extension function
fun [Ação][Recurso]Request.toInput([parametrosAdicionais]): [Ação][Recurso]Input {
    return [Ação][Recurso]Input(
        [campo] = this.[campo],
        [campoOpcional] = this.[campoOpcional],
        [SE RECEBER PARÂMETROS ADICIONAIS]
        [headerParam] = [headerParam]
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

@Schema(description = "[Descrição do response]")
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

#### Context-Specific Exception Handler
```kotlin
package [package].web.[contexto].exception

import [package].shared.exception.*
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

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Business error: {}", ex.message)

        val status = when (ex) {
            is [Contexto]NotFoundException -> HttpStatus.NOT_FOUND
            is [Contexto]ValidationException -> HttpStatus.BAD_REQUEST
            is [Contexto]BusinessRuleException -> HttpStatus.FORBIDDEN
            [OUTRAS EXCEÇÕES ESPECÍFICAS]
            else -> HttpStatus.BAD_REQUEST
        }

        return buildErrorResponse(status, ex.message ?: "Business error", request)
    }

    @ExceptionHandler(InfrastructureException::class)
    fun handleInfrastructureException(
        ex: InfrastructureException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Infrastructure error [${ex.component}]: ${ex.message}", ex)

        val message = "Service temporarily unavailable. Please try again later."
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, message, request)
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

## ⚠️ PONTOS DE ATENÇÃO

### Controllers
- **Thin controllers**: Sem lógica de negócio
- **Implementam interfaces**: Documentação separada
- **Validação adicional**: Headers vazios (Spring não valida)
- **Logging apropriado**: INFO para fluxo, sem dados sensíveis

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
- **Exemplos realistas**: Para cada response code
- **Descrições claras**: Em inglês
- **Pattern informativo**: Validação real no backend
- **Tags organizadas**: Por contexto de negócio

### Padrões da Empresa
- **Correlation ID**: Já existe no filter (INITIAL-SETUP)
- **No JsonProperty**: Apenas quando nome diferente
- **Mínimo comentários**: Código auto-explicativo
- **Headers lowercase**: Padrão HTTP

---

### FEEDBACK
<!-- Registro de melhorias durante uso -->

### NOTAS DE VERSÃO
- v1.0.0: Camada Presentation com controllers finos, Swagger separado e tratamento de exceções