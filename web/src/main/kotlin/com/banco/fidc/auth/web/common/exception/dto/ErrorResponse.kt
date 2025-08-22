package com.banco.fidc.auth.web.common.exception.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Standard API error response")
data class ErrorResponse(
    @Schema(description = "Error timestamp", example = "2025-08-22T10:30:00")
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