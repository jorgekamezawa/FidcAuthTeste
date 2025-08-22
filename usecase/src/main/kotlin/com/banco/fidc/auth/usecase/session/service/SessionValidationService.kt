package com.banco.fidc.auth.usecase.session.service

import com.banco.fidc.auth.domain.session.repository.UserSessionControlRepository
import com.banco.fidc.auth.usecase.session.configprovider.SessionConfigProvider
import com.banco.fidc.auth.usecase.session.exception.SessionValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class SessionValidationService(
    private val userSessionControlRepository: UserSessionControlRepository,
    private val sessionConfigProvider: SessionConfigProvider
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    fun validateCpf(cpf: String) {
        logger.debug("Validando CPF: ${cpf.take(3)}***${cpf.takeLast(2)}")
        
        if (cpf.length != 11 || !cpf.all { it.isDigit() }) {
            throw SessionValidationException("CPF inválido")
        }
    }
    
    fun validateJwtClaims(claims: Map<String, Any>): String {
        logger.debug("Validando claims do JWT")
        
        val cpf = claims["cpf"] as? String
        if (cpf.isNullOrBlank()) {
            throw SessionValidationException("Token JWT não contém CPF válido")
        }
        
        validateCpf(cpf)
        return cpf
    }
    
    fun generateSessionSecret(): String {
        val secretLength = sessionConfigProvider.getSecretLength()
        return UUID.randomUUID().toString().take(secretLength)
    }
}