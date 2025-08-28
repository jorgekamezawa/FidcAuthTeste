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

    // Grupo 1: Validação de Entrada (400) - Usar ex.message
    @ExceptionHandler(SessionValidationException::class, InvalidInputException::class)
    fun handleValidationErrors(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Session validation error on {}: {}", request.requestURI, ex.message)
        
        val message = ex.message ?: "Invalid request data"
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    // Grupo 2: Recursos Não Encontrados (404) - Usar ex.message  
    @ExceptionHandler(SessionNotFoundException::class)
    fun handleNotFoundErrors(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Session not found on {}: {}", request.requestURI, ex.message)
        
        val message = ex.message ?: "Session not found or expired"
        return buildErrorResponse(HttpStatus.NOT_FOUND, message, request)
    }

    // Grupo 3: Serviços Indisponíveis (503) - Mensagem customizada
    @ExceptionHandler(UserManagementIntegrationException::class, FidcPermissionIntegrationException::class)
    fun handleServiceUnavailableErrors(
        ex: BusinessException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Service integration failure on {}: {}", request.requestURI, ex.message, ex)
        
        val message = when (ex) {
            is UserManagementIntegrationException -> "Serviço de usuários temporariamente indisponível"
            is FidcPermissionIntegrationException -> "Serviço de permissões temporariamente indisponível"
            else -> "Serviço temporariamente indisponível"
        }
        
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, message, request)
    }

    // Grupo 4: Erros Internos (500) - Mensagem padronizada + logging completo
    @ExceptionHandler(SessionProcessingException::class)
    fun handleInternalProcessingErrors(
        ex: SessionProcessingException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error(
            "Session processing failure on {} - Request: {} - Error: {}", 
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
            "Redis", "RedisRepository", "PostgreSQL", "SessionRepository" -> 
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