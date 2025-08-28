package com.banco.fidc.auth.web.session.documentation

import com.banco.fidc.auth.web.common.exception.dto.ErrorResponse
import com.banco.fidc.auth.web.common.exception.dto.ValidationErrorResponse
import com.banco.fidc.auth.web.session.dto.request.CreateUserSessionRequest
import com.banco.fidc.auth.web.session.dto.response.CreateUserSessionResponse
import com.banco.fidc.auth.web.session.dto.response.GetJwtSecretResponse
import com.banco.fidc.auth.web.session.dto.response.SelectRelationshipResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader

@Tag(
    name = "Autenticação",
    description = "Operações de gerenciamento de sessão de usuário e autenticação"
)
interface SessionApiDoc {

    @Operation(
        summary = "Criar sessão de usuário",
        description = "Cria uma nova sessão autenticada de usuário com dados do usuário, permissões e token de acesso"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Sessão criada com sucesso",
            content = [Content(
                schema = Schema(implementation = CreateUserSessionResponse::class),
                examples = [ExampleObject(
                    name = "Sucesso",
                    value = """{
                        "userInfo": {
                            "cpf": "123***456-78",
                            "fullName": "João Silva Santos",
                            "email": "j***@email.com",
                            "birthDate": "1985-05-15",
                            "phoneNumber": "(11) 9****-1234"
                        },
                        "fund": {
                            "id": "fund-123",
                            "name": "FIDC ABC",
                            "type": "FIDC"
                        },
                        "relationshipList": [
                            {
                                "id": "rel-456",
                                "type": "CEDENTE",
                                "name": "Empresa XYZ Ltda",
                                "status": "ACTIVE",
                                "contractNumber": "CTR-001"
                            }
                        ],
                        "permissions": ["READ_PORTFOLIO", "MANAGE_RECEIVABLES"],
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    }"""
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
                        "path": "/v1/sessions",
                        "errors": {
                            "signedData": "signedData é obrigatório"
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
                        "path": "/v1/sessions"
                    }"""
                )]
            )]
        )
    ])
    fun createUserSession(
        @RequestBody request: CreateUserSessionRequest,

        @Parameter(description = "Identificador do parceiro", required = true, example = "prevcom")
        @RequestHeader("partner") partner: String,

        @Parameter(description = "String do user agent do cliente", required = true, example = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        @RequestHeader("user-agent") userAgent: String,

        @Parameter(description = "Canal de acesso (WEB, MOBILE, etc.)", required = true, example = "WEB")
        @RequestHeader("channel") channel: String,

        @Parameter(description = "Impressão digital do dispositivo", required = true, example = "fp_abc123def456")
        @RequestHeader("fingerprint") fingerprint: String,

        @Parameter(description = "Latitude da localização do usuário (opcional - salvo como null se não fornecido)", required = false, example = "-23.550520")
        @RequestHeader("latitude", required = false) latitude: String?,

        @Parameter(description = "Longitude da localização do usuário (opcional - salvo como null se não fornecido)", required = false, example = "-46.633309")
        @RequestHeader("longitude", required = false) longitude: String?,

        @Parameter(description = "Precisão da localização em metros (opcional - salvo como null se não fornecido)", required = false, example = "10")
        @RequestHeader("location-accuracy", required = false) locationAccuracy: String?,

        @Parameter(description = "Timestamp da localização em formato ISO (opcional - salvo como null se não fornecido)", required = false, example = "2025-08-28T14:45:32Z")
        @RequestHeader("location-timestamp", required = false) locationTimestamp: String?,

        @Parameter(description = "ID de correlação de rastreamento", required = false, example = "req_123456789")
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,

        httpRequest: HttpServletRequest
    ): CreateUserSessionResponse

    @Operation(
        summary = "Selecionar relacionamento",
        description = "Seleciona um relacionamento específico, busca permissões contextuais e atualiza a sessão. " +
                "O cabeçalho partner deve coincidir com o partner da sessão atual para validação de segurança."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Relacionamento selecionado com sucesso",
            content = [Content(
                schema = Schema(implementation = SelectRelationshipResponse::class),
                examples = [ExampleObject(
                    name = "Sucesso",
                    value = """{
                        "userInfo": {
                            "cpf": "123***456-78",
                            "fullName": "João Silva Santos",
                            "email": "j***@email.com",
                            "birthDate": "1985-05-15",
                            "phoneNumber": "(11) 9****-1234"
                        },
                        "fund": {
                            "id": "fund-123",
                            "name": "FIDC ABC",
                            "type": "FIDC"
                        },
                        "relationshipList": [
                            {
                                "id": "rel-456",
                                "type": "CEDENTE",
                                "name": "Empresa XYZ Ltda",
                                "status": "ACTIVE",
                                "contractNumber": "CTR-001"
                            }
                        ],
                        "relationshipSelected": {
                            "id": "rel-456",
                            "type": "CEDENTE",
                            "name": "Empresa XYZ Ltda",
                            "status": "ACTIVE",
                            "contractNumber": "CTR-001"
                        },
                        "permissions": ["VIEW_PLAN_DETAILS", "REQUEST_LOAN"],
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "default",
            description = "Erros gerais - Retorna ErrorResponse para códigos 400, 401, 403, 404, 503, etc.",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "ErrorResponse",
                    value = """{
                        "timestamp": "2025-08-28T14:45:32",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Header relationshipId é obrigatório",
                        "path": "/v1/sessions/relationship"
                    }"""
                )]
            )]
        )
    ])
    fun selectRelationship(
        @Parameter(description = "Token Bearer da criação da sessão", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @RequestHeader("authorization") authorization: String,

        @Parameter(description = "Identificador do parceiro (deve coincidir com o partner da sessão)", required = true, example = "btg")
        @RequestHeader("partner") partner: String,

        @Parameter(description = "ID do relacionamento a selecionar", required = true, example = "rel-456")
        @RequestHeader("relationshipId") relationshipId: String,

        @Parameter(description = "ID de correlação de rastreamento", required = false, example = "req_987654321")
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,

        httpRequest: HttpServletRequest
    ): SelectRelationshipResponse

    @Operation(
        summary = "Finalizar sessão",
        description = "Finaliza a sessão ativa do usuário, invalidando o AccessToken e limpando o estado da sessão. " +
                "O cabeçalho partner deve coincidir com o partner da sessão sendo finalizada. Esta operação é idempotente."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "Sessão finalizada com sucesso"
        ),
        ApiResponse(
            responseCode = "default",
            description = "Erros gerais - Retorna ErrorResponse para códigos 400, 401, 403, 500, 503, etc.",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "ErrorResponse",
                    value = """{
                        "timestamp": "2025-08-28T14:45:32",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Header partner é obrigatório",
                        "path": "/v1/sessions"
                    }"""
                )]
            )]
        )
    ])
    fun endSession(
        @Parameter(description = "Token Bearer obtido do fluxo de autenticação", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @RequestHeader("Authorization") authorization: String,

        @Parameter(description = "Identificador do parceiro (prevcom, caio, etc.)", required = true, example = "btg")
        @RequestHeader("partner") partner: String,

        @Parameter(description = "ID de correlação de rastreamento (auto-gerado se omitido)", required = false, example = "req_endSession_123")
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,

        httpRequest: HttpServletRequest
    ): ResponseEntity<Void>

    @Operation(
        summary = "Obter Segredo JWT",
        description = "Recupera o hash do segredo de assinatura JWT para validação de token. Este endpoint é usado para configuração inicial e não requer autenticação."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Segredo JWT recuperado com sucesso",
            content = [Content(
                schema = Schema(implementation = GetJwtSecretResponse::class),
                examples = [ExampleObject(
                    name = "Sucesso",
                    value = """{
                        "secret": "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456"
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "default",
            description = "Erros gerais - Retorna ErrorResponse para códigos 500, 503, etc.",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "ErrorResponse",
                    value = """{
                        "timestamp": "2025-08-28T14:45:32",
                        "status": 500,
                        "error": "Internal Server Error",
                        "message": "Erro interno do servidor",
                        "path": "/v1/sessions/jwt-secret"
                    }"""
                )]
            )]
        )
    ])
    fun getJwtSecret(
        @Parameter(description = "ID de correlação de rastreamento (auto-gerado se omitido)", required = false, example = "req_getSecret_456")
        @RequestHeader("x-correlation-id", required = false) correlationId: String?
    ): GetJwtSecretResponse
}