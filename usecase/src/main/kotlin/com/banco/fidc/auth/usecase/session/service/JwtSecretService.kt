package com.banco.fidc.auth.usecase.session.service

interface JwtSecretService {
    fun getJwtSecret(): String
    fun validateJwtToken(token: String): Map<String, Any>
    fun generateAccessToken(sessionId: String, sessionSecret: String, expirationSeconds: Long): String
}