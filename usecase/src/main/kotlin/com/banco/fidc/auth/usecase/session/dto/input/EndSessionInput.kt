package com.banco.fidc.auth.usecase.session.dto.input

data class EndSessionInput(
    val accessToken: String,
    val partner: String,
    val clientIpAddress: String,
    val userAgent: String
)