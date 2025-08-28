package com.banco.fidc.auth.usecase.session.service

interface JwtSecretService {
    fun validateJwtToken(token: String): Map<String, Any>
    fun validateJwtTokenWithSecret(token: String, secret: String)
    fun generateAccessToken(sessionId: String, sessionSecret: String, expirationSeconds: Long): String
    fun getSecretHash(): String
}