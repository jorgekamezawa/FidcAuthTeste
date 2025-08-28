package com.banco.fidc.auth.web.session.exception

import com.banco.fidc.auth.shared.exception.BusinessException
import com.banco.fidc.auth.shared.exception.InfrastructureException
import com.banco.fidc.auth.usecase.session.exception.*
import com.banco.fidc.auth.web.common.exception.dto.ErrorResponse
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
class SessionExceptionHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Session business error: {}", ex.message)

        val (status, message) = when (ex) {
            is SessionValidationException -> HttpStatus.BAD_REQUEST to (ex.message ?: "Session validation error")
            is SessionNotFoundException -> HttpStatus.NOT_FOUND to "Session not found or expired"
            is SessionProcessingException -> HttpStatus.INTERNAL_SERVER_ERROR to "Error processing session request"
            is UserManagementIntegrationException -> HttpStatus.SERVICE_UNAVAILABLE to "User service temporarily unavailable"
            is FidcPermissionIntegrationException -> HttpStatus.SERVICE_UNAVAILABLE to "Permission service temporarily unavailable"
            else -> HttpStatus.BAD_REQUEST to (ex.message ?: "Session validation error")
        }

        return buildErrorResponse(status, message, request)
    }

    @ExceptionHandler(InfrastructureException::class)
    fun handleInfrastructureException(
        ex: InfrastructureException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Session infrastructure error [${ex.component}]: ${ex.message}", ex)

        val (status, message) = when (ex.component) {
            "UserManagement" -> {
                when {
                    ex.message?.contains("Usuário não encontrado", ignoreCase = true) == true ||
                    ex.message?.contains("User not found", ignoreCase = true) == true ||
                    ex.message?.contains("Partner não encontrado", ignoreCase = true) == true ||
                    ex.message?.contains("Partner not found", ignoreCase = true) == true -> 
                        HttpStatus.BAD_REQUEST to "Invalid user data"
                    ex.message?.contains("não autorizado", ignoreCase = true) == true ||
                    ex.message?.contains("unauthorized", ignoreCase = true) == true -> 
                        HttpStatus.FORBIDDEN to "User not authorized"
                    else -> HttpStatus.SERVICE_UNAVAILABLE to "User service temporarily unavailable"
                }
            }
            "FidcPermission" -> {
                when {
                    ex.message?.contains("não encontrado", ignoreCase = true) == true ||
                    ex.message?.contains("not found", ignoreCase = true) == true -> 
                        HttpStatus.NOT_FOUND to "Permissions not found"
                    ex.message?.contains("não autorizado", ignoreCase = true) == true ||
                    ex.message?.contains("unauthorized", ignoreCase = true) == true -> 
                        HttpStatus.FORBIDDEN to "Permission denied"
                    else -> HttpStatus.SERVICE_UNAVAILABLE to "Permission service temporarily unavailable"
                }
            }
            "JwtSecret" -> {
                when {
                    ex.message?.contains("expired", ignoreCase = true) == true -> 
                        HttpStatus.UNAUTHORIZED to "Token expired"
                    ex.message?.contains("invalid", ignoreCase = true) == true -> 
                        HttpStatus.UNAUTHORIZED to "Invalid token"
                    ex.message?.contains("malformed", ignoreCase = true) == true -> 
                        HttpStatus.BAD_REQUEST to "Malformed token"
                    else -> HttpStatus.SERVICE_UNAVAILABLE to "Authentication service temporarily unavailable"
                }
            }
            "Redis", "RedisRepository" -> HttpStatus.SERVICE_UNAVAILABLE to "Session service temporarily unavailable"
            "PostgreSQL", "SessionRepository" -> HttpStatus.SERVICE_UNAVAILABLE to "Database temporarily unavailable"
            else -> HttpStatus.SERVICE_UNAVAILABLE to "Service temporarily unavailable"
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