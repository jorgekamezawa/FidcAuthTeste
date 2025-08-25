package com.banco.fidc.auth.usecase.session.service

import com.banco.fidc.auth.domain.session.entity.Session
import com.banco.fidc.auth.usecase.session.configprovider.SessionConfigProvider
import com.banco.fidc.auth.usecase.session.exception.SessionValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class SessionValidationService(
    private val sessionConfigProvider: SessionConfigProvider
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    fun validateCpf(cpf: String) {
        logger.debug("Validando CPF: ${cpf.take(3)}***${cpf.takeLast(2)}")
        
        if (cpf.length != 11 || !cpf.all { it.isDigit() }) {
            throw SessionValidationException("CPF inválido")
        }
    }
    
    /**
     * Extrai uma claim específica do JWT e valida se está presente
     */
    fun extractRequiredClaimFromJwt(claims: Map<String, Any>, claimName: String): String {
        logger.debug("Extraindo claim '$claimName' do JWT")
        
        val claimValue = claims[claimName] as? String
        if (claimValue.isNullOrBlank()) {
            throw SessionValidationException("Token JWT não contém '$claimName' válido")
        }
        
        return claimValue
    }
    
    /**
     * Extrai CPF do JWT e o valida (método de conveniência para fluxo 1)
     */
    fun validateAndExtractCpfFromJwt(claims: Map<String, Any>): String {
        val cpf = extractRequiredClaimFromJwt(claims, "cpf")
        validateCpf(cpf)
        return cpf
    }
    
    fun generateSessionSecret(): String {
        val secretLength = sessionConfigProvider.getSecretLength()
        return UUID.randomUUID().toString().take(secretLength)
    }
    
    /**
     * Valida se um relacionamento existe na sessão e está ativo
     */
    fun validateRelationshipExists(session: Session, relationshipId: String) {
        logger.debug("Validando existência do relacionamento: $relationshipId")
        
        val relationship = session.relationshipList.find { it.id == relationshipId }
        if (relationship == null) {
            throw SessionValidationException("Relacionamento não encontrado na sessão")
        }
        
        if (relationship.status != "ACTIVE") {
            throw SessionValidationException("Relacionamento inativo")
        }
    }
    
    /**
     * Extrai claims do JWT sem validar assinatura (apenas para leitura)
     */
    fun extractClaimsFromJwt(accessToken: String): Map<String, Any> {
        logger.debug("Extraindo claims do JWT")
        
        // Remover "Bearer " se presente
        val token = if (accessToken.startsWith("Bearer ", ignoreCase = true)) {
            accessToken.substring(7)
        } else {
            accessToken
        }
        
        // Decodificar JWT sem validar assinatura
        val parts = token.split(".")
        if (parts.size != 3) {
            throw SessionValidationException("Token de acesso inválido")
        }
        
        try {
            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            return parseJwtPayload(payload)
        } catch (e: Exception) {
            logger.error("Erro ao extrair claims do JWT", e)
            throw SessionValidationException("Token de acesso malformado")
        }
    }
    
    /**
     * Extrai sessionId do AccessToken (método de conveniência para fluxo 2)
     */
    fun extractSessionIdFromToken(accessToken: String): String {
        val claims = extractClaimsFromJwt(accessToken)
        return extractRequiredClaimFromJwt(claims, "sessionId")
    }
    
    /**
     * Valida se o token não expirou
     */
    fun validateTokenExpiration(claims: Map<String, Any>) {
        logger.debug("Validando expiração do token")
        
        val exp = claims["exp"] as? Long
        if (exp == null) {
            throw SessionValidationException("Token de acesso inválido")
        }
        
        val currentTime = System.currentTimeMillis() / 1000
        if (currentTime > exp) {
            throw SessionValidationException("Token de acesso expirado")
        }
    }
    
    /**
     * Parse simples do payload JWT (JSON)
     */
    private fun parseJwtPayload(payload: String): Map<String, Any> {
        // Parse manual básico do JSON - pode ser substituído por Jackson se necessário
        val claims = mutableMapOf<String, Any>()
        
        // Extrair sessionId
        Regex("\"sessionId\":\\s*\"([^\"]+)\"").find(payload)?.let {
            claims["sessionId"] = it.groupValues[1]
        }
        
        // Extrair exp
        Regex("\"exp\":\\s*(\\d+)").find(payload)?.let {
            claims["exp"] = it.groupValues[1].toLong()
        }
        
        // Extrair cpf (para fluxo 1)
        Regex("\"cpf\":\\s*\"([^\"]+)\"").find(payload)?.let {
            claims["cpf"] = it.groupValues[1]
        }
        
        return claims
    }
}