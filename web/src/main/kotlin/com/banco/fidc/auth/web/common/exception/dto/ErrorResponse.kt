package com.banco.fidc.auth.web.common.exception.dto

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