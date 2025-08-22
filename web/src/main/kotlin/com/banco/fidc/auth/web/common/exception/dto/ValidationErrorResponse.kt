package com.banco.fidc.auth.web.common.exception.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Validation error response with field details")
data class ValidationErrorResponse(
    @Schema(description = "Error timestamp", example = "2025-08-22T10:30:00")
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