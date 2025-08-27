package com.banco.fidc.auth.usecase.session.dto.input

data class CreateUserSessionInput(
    val signedData: String,
    val partner: String,
    val userAgent: String,
    val channel: String,
    val fingerprint: String,
    val latitude: String?,
    val longitude: String?,
    val locationAccuracy: String?,
    val locationTimestamp: String?,
    val clientIpAddress: String
)