package com.banco.fidc.auth.usecase.session.service

interface JwtParsingService {
    fun extractClaims(token: String, requiredFields: List<String>): Map<String, Any>
}