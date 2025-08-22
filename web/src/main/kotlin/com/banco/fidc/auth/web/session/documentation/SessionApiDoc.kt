package com.banco.fidc.auth.web.session.documentation

import com.banco.fidc.auth.web.common.exception.dto.ErrorResponse
import com.banco.fidc.auth.web.session.dto.request.CreateUserSessionRequest
import com.banco.fidc.auth.web.session.dto.response.CreateUserSessionResponse
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
                        "path": "/api/v1/auth/session"
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
        @RequestHeader("x-correlation-id", required = false) correlationId: String?
    ): CreateUserSessionResponse
}