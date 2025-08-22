package com.banco.fidc.auth.usecase.session.dto.params

data class RateLimitCheckParams(
    val clientIpAddress: String,
    val userAgent: String
)