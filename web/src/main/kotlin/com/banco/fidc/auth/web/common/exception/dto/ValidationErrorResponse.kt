package com.banco.fidc.auth.web.common.exception.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

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