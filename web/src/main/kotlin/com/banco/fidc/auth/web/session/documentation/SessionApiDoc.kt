package com.banco.fidc.auth.web.session.documentation

import com.banco.fidc.auth.web.common.exception.dto.ErrorResponse
import com.banco.fidc.auth.web.session.dto.request.CreateUserSessionRequest
import com.banco.fidc.auth.web.session.dto.response.CreateUserSessionResponse
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
    name = "Authentication",
    description = "User session management and authentication operations"
)
interface SessionApiDoc {

    @Operation(
        summary = "Create user session",
        description = "Creates a new authenticated user session with user data, permissions and access token"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Session created successfully",
            content = [Content(
                schema = Schema(implementation = CreateUserSessionResponse::class),
                examples = [ExampleObject(
                    name = "Success",
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
            responseCode = "400",
            description = "Bad Request - Invalid input data or JWT token",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    value = """{
                        "timestamp": "2025-08-22T14:45:32",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Invalid JWT token",
                        "path": "/v1/sessions"
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "403",
            description = "Forbidden - User not authorized or rate limit exceeded",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found - User not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "503",
            description = "Service Unavailable - External service temporarily unavailable",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun createUserSession(
        @RequestBody request: CreateUserSessionRequest,
        
        @Parameter(description = "Partner identifier", required = true)
        @RequestHeader("partner") partner: String,
        
        @Parameter(description = "User agent string from client", required = true)
        @RequestHeader("user-agent") userAgent: String,
        
        @Parameter(description = "Access channel (WEB, MOBILE, etc.)", required = true)
        @RequestHeader("channel") channel: String,
        
        @Parameter(description = "Device fingerprint", required = true)
        @RequestHeader("fingerprint") fingerprint: String,
        
        @Parameter(description = "User location latitude", required = true)
        @RequestHeader("latitude") latitude: String,
        
        @Parameter(description = "User location longitude", required = true)
        @RequestHeader("longitude") longitude: String,
        
        @Parameter(description = "Location accuracy in meters", required = true)
        @RequestHeader("location-accuracy") locationAccuracy: String,
        
        @Parameter(description = "Location timestamp in ISO format", required = true)
        @RequestHeader("location-timestamp") locationTimestamp: String,
        
        @Parameter(description = "Tracking correlation ID", required = false)
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,
        
        httpRequest: HttpServletRequest
    ): CreateUserSessionResponse

    @Operation(
        summary = "Select relationship",
        description = "Selects a specific relationship, fetches contextual permissions and updates session"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Relationship selected successfully",
            content = [Content(
                schema = Schema(implementation = SelectRelationshipResponse::class),
                examples = [ExampleObject(
                    name = "Success",
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
            responseCode = "400",
            description = "Bad Request - Invalid relationship ID or missing headers",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    value = """{
                        "timestamp": "2025-08-22T14:45:32",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Header relationshipId é obrigatório",
                        "path": "/v1/sessions/relationship"
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or expired access token",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Not Found - Session not found or expired",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "429",
            description = "Too Many Requests - Rate limit exceeded",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "503",
            description = "Service Unavailable - FidcPermission service temporarily unavailable",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    ])
    fun selectRelationship(
        @Parameter(description = "Bearer token from session creation", required = true)
        @RequestHeader("authorization") authorization: String,
        
        @Parameter(description = "ID of relationship to select", required = true)
        @RequestHeader("relationshipId") relationshipId: String,
        
        @Parameter(description = "Tracking correlation ID", required = false)
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,
        
        httpRequest: HttpServletRequest
    ): SelectRelationshipResponse

    @Operation(
        summary = "End session",
        description = "Ends the active user session, invalidating the AccessToken and clearing session state. This operation is idempotent."
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "204",
            description = "Session ended successfully"
        ),
        ApiResponse(
            responseCode = "400",
            description = "Bad Request - Missing partner header or malformed token",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "Missing partner header",
                    value = """{
                        "timestamp": "2025-08-18T14:45:32",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Header partner é obrigatório",
                        "path": "/v1/sessions"
                    }"""
                ), ExampleObject(
                    name = "Malformed token",
                    value = """{
                        "timestamp": "2025-08-18T14:45:32",
                        "status": 400,
                        "error": "Bad Request",
                        "message": "Token de acesso malformado",
                        "path": "/v1/sessions"
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid token signature or missing Authorization header",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "Invalid token",
                    value = """{
                        "timestamp": "2025-08-18T14:45:32",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Token de acesso inválido",
                        "path": "/v1/sessions"
                    }"""
                ), ExampleObject(
                    name = "Missing authorization header",
                    value = """{
                        "timestamp": "2025-08-18T14:45:32",
                        "status": 401,
                        "error": "Unauthorized",
                        "message": "Token de acesso obrigatório",
                        "path": "/v1/sessions"
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "403",
            description = "Forbidden - Partner not authorized for this session",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "Partner not authorized",
                    value = """{
                        "timestamp": "2025-08-18T14:45:32",
                        "status": 403,
                        "error": "Forbidden",
                        "message": "Partner não autorizado para esta sessão",
                        "path": "/v1/sessions"
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "429",
            description = "Too Many Requests - Rate limit exceeded",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "Rate limit exceeded",
                    value = """{
                        "timestamp": "2025-08-18T14:45:32",
                        "status": 429,
                        "error": "Too Many Requests",
                        "message": "Rate limit excedido",
                        "path": "/v1/sessions"
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "Internal error",
                    value = """{
                        "timestamp": "2025-08-18T14:45:32",
                        "status": 500,
                        "error": "Internal Server Error",
                        "message": "Erro interno do servidor",
                        "path": "/v1/sessions"
                    }"""
                )]
            )]
        ),
        ApiResponse(
            responseCode = "503",
            description = "Service Unavailable - Redis temporarily unavailable",
            content = [Content(
                schema = Schema(implementation = ErrorResponse::class),
                examples = [ExampleObject(
                    name = "Redis unavailable",
                    value = """{
                        "timestamp": "2025-08-18T14:45:32",
                        "status": 503,
                        "error": "Service Unavailable",
                        "message": "Serviço temporariamente indisponível",
                        "path": "/v1/sessions"
                    }"""
                )]
            )]
        )
    ])
    fun endSession(
        @Parameter(description = "Bearer token obtained from authentication flow", required = true)
        @RequestHeader("Authorization") authorization: String,
        
        @Parameter(description = "Partner identifier (prevcom, caio, etc.)", required = true)
        @RequestHeader("partner") partner: String,
        
        @Parameter(description = "Tracking correlation ID (auto-generated if omitted)", required = false)
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,
        
        httpRequest: HttpServletRequest
    ): ResponseEntity<Void>
}