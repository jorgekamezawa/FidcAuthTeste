package com.banco.fidc.auth.usecase.session.service

import com.banco.fidc.auth.usecase.session.exception.SessionValidationException
import org.springframework.stereotype.Service

@Service
class SessionValidationService(
    private val jwtParsingService: JwtParsingService
) {

    fun validateCpf(cpf: String) {
        if (cpf.length != 11 || !cpf.all { it.isDigit() }) {
            throw SessionValidationException("CPF inválido")
        }
    }

    fun extractRequiredClaimFromJwt(claims: Map<String, Any>, claimName: String): String {
        val claimValue = claims[claimName] as? String
        if (claimValue.isNullOrBlank()) {
            throw SessionValidationException("Token JWT não contém '$claimName' válido")
        }
        return claimValue
    }

    fun validateAndExtractCpfFromClaims(claims: Map<String, Any>): String {
        val cpf = extractRequiredClaimFromJwt(claims, "cpf")
        validateCpf(cpf)
        return cpf
    }

    fun extractSessionIdFromToken(accessToken: String): String {
        val claims = jwtParsingService.extractClaims(accessToken, listOf("sessionId"))
        return extractRequiredClaimFromJwt(claims, "sessionId")
    }

    fun extractCpfFromToken(accessToken: String): String {
        val claims = jwtParsingService.extractClaims(accessToken, listOf("cpf"))
        return validateAndExtractCpfFromClaims(claims)
    }
}