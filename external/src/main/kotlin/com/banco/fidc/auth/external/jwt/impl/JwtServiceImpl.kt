package com.banco.fidc.auth.external.jwt.impl

import com.banco.fidc.auth.external.jwt.exception.JwtSecretException
import com.banco.fidc.auth.external.jwt.service.JwtSecretResolver
import com.banco.fidc.auth.usecase.session.exception.SessionValidationException
import com.banco.fidc.auth.usecase.session.service.JwtParsingService
import com.banco.fidc.auth.usecase.session.service.JwtSecretService as JwtSecretServiceInterface
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class JwtServiceImpl(
    private val jwtSecretResolver: JwtSecretResolver,
    private val objectMapper: ObjectMapper
) : JwtSecretServiceInterface, JwtParsingService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // ========== JWT Secret Service Methods ==========

    override fun validateJwtToken(token: String): Map<String, Any> {
        logger.debug("Validating JWT token")
        
        return try {
            val secret = jwtSecretResolver.getJwtSigningKey()
            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(cleanBearerToken(token))
                .payload
            
            logger.debug("JWT token validated successfully")
            claims.toMap()
            
        } catch (e: Exception) {
            logger.error("Error validating JWT token: ${e.message}")
            throw JwtSecretException("Invalid JWT token", e)
        }
    }

    override fun validateJwtTokenWithSecret(token: String, secret: String) {
        logger.debug("Validating JWT token with specific secret")
        
        return try {
            val cleanToken = cleanBearerToken(token)
            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(cleanToken)
                
            logger.debug("JWT token validated successfully with specific secret")

        } catch (e: Exception) {
            logger.error("Error validating JWT token with specific secret: ${e.message}")
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

    override fun getSecretHash(): String {
        logger.debug("Getting JWT secret hash")
        
        return try {
            val secret = jwtSecretResolver.getJwtSigningKey()
            logger.debug("JWT secret hash retrieved successfully")
            secret
            
        } catch (e: Exception) {
            logger.error("Error getting JWT secret hash: ${e.message}")
            throw JwtSecretException("Failed to get JWT secret", e)
        }
    }

    // ========== JWT Parsing Service Methods ==========

    override fun extractClaims(token: String, requiredFields: List<String>): Map<String, Any> {
        val cleanToken = cleanBearerToken(token)
        val jwtParts = cleanToken.split(".")

        if (jwtParts.size != 3) {
            throw SessionValidationException("Token de acesso inválido")
        }

        return try {
            val payload = String(Base64.getUrlDecoder().decode(jwtParts[1]))
            parseJwtPayload(payload, requiredFields)
        } catch (e: Exception) {
            logger.error("Erro ao extrair claims do JWT", e)
            throw SessionValidationException("Token de acesso malformado")
        }
    }

    // ========== Private Helper Methods ==========

    private fun cleanBearerToken(accessToken: String): String {
        return if (accessToken.startsWith("Bearer ", ignoreCase = true)) {
            accessToken.substring(7)
        } else {
            accessToken
        }
    }

    private fun parseJwtPayload(payload: String, requiredFields: List<String>): Map<String, Any> {
        val allClaims: Map<String, Any> = objectMapper.readValue(payload)

        return requiredFields.mapNotNull { fieldName ->
            allClaims[fieldName]?.let { fieldName to it }
        }.toMap()
    }
}