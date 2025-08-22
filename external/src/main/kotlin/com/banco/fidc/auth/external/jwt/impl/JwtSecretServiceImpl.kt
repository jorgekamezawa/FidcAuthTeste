package com.banco.fidc.auth.external.jwt.impl

import com.banco.fidc.auth.external.jwt.exception.JwtSecretException
import com.banco.fidc.auth.usecase.session.service.JwtSecretService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtSecretServiceImpl(
    @Value("\${jwt.fallback-secret:default-secret-for-development-only-change-in-production}")
    private val fallbackSecret: String
) : JwtSecretService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getJwtSecret(): String {
        // TODO: QUANDO AWS ESTIVER CONFIGURADA, DESCOMENTE AS LINHAS ABAIXO E COMENTE O FALLBACK
        // return tryGetFromAwsSecrets() ?: tryGetFromFidcPasswordApi() ?: fallbackSecret
        
        // MOCK: USANDO FALLBACK PARA DESENVOLVIMENTO
        logger.warn("USANDO SECRET MOCKADA - AWS Secrets Manager não disponível ainda")
        return fallbackSecret
    }

    override fun validateJwtToken(token: String): Map<String, Any> {
        logger.debug("Validating JWT token")
        
        return try {
            val secret = getJwtSecret()
            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            
            logger.debug("JWT token validated successfully")
            claims.toMap()
            
        } catch (e: Exception) {
            logger.error("Error validating JWT token: ${e.message}")
            throw JwtSecretException("Invalid JWT token", e)
        }
    }

    override fun generateAccessToken(sessionId: String, sessionSecret: String, expirationSeconds: Long): String {
        logger.debug("Generating access token for sessionId: $sessionId")
        
        return try {
            val key = Keys.hmacShaKeyFor(sessionSecret.toByteArray())
            val now = Instant.now()
            val expiration = now.plus(expirationSeconds, ChronoUnit.SECONDS)
            
            val token = Jwts.builder()
                .claim("sessionId", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact()
            
            logger.debug("Access token generated successfully")
            token
            
        } catch (e: Exception) {
            logger.error("Error generating access token: ${e.message}")
            throw JwtSecretException("Failed to generate access token", e)
        }
    }
    
    // MOCK: 3 ESTRATÉGIAS DE FALLBACK - IMPLEMENTAR QUANDO AS INTEGRAÇÕES ESTIVEREM PRONTAS
    private fun tryGetFromAwsSecrets(): String? {
        return try {
            logger.debug("Tentando buscar JWT secret do AWS Secrets Manager...")
            // TODO: Implementar busca no AWS Secrets Manager
            // secretsManagerClient.getSecretValue(...)
            null // Retorna null para usar próximo fallback
        } catch (e: Exception) {
            logger.warn("Erro ao buscar secret do AWS: ${e.message}")
            null
        }
    }
    
    private fun tryGetFromFidcPasswordApi(): String? {
        return try {
            logger.debug("Tentando buscar JWT secret do FidcPassword API...")
            // TODO: Implementar chamada para FidcPassword API
            // fidcPasswordClient.getJwtSecret()
            null // Retorna null para usar fallback
        } catch (e: Exception) {
            logger.warn("Erro ao buscar secret do FidcPassword: ${e.message}")
            null
        }
    }
}