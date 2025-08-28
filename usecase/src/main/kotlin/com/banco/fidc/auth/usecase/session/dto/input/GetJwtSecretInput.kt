package com.banco.fidc.auth.usecase.session.dto.input

data class GetJwtSecretInput(
    val userAgent: String,
    val clientIpAddress: String
)