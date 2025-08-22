package com.banco.fidc.auth.external.jwt.impl

import com.banco.fidc.auth.external.jwt.exception.JwtSecretException
import com.banco.fidc.auth.usecase.session.service.JwtSecretService
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtSecretServiceImpl(
    private val secretsManagerClient: SecretsManagerClient,
    @Value("\${aws.secret-manager.jwt.name}")
    private val jwtSecretName: String,
    @Value("\${jwt.fallback-secret}")
    private val fallbackSecret: String
) : JwtSecretService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getJwtSecret(): String {
        logger.debug("Buscando JWT secret do AWS Secrets Manager")
        return tryGetFromAwsSecrets() ?: tryGetFromFidcPasswordApi() ?: fallbackSecret
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
    
    private fun tryGetFromAwsSecrets(): String? {
        return try {
            logger.debug("Tentando buscar JWT secret do AWS Secrets Manager: $jwtSecretName")
            
            val request = GetSecretValueRequest.builder()
                .secretId(jwtSecretName)
                .build()
                
            val response = secretsManagerClient.getSecretValue(request)
            val secretString = response.secretString()
            
            if (secretString.isNullOrBlank()) {
                logger.warn("Secret string está vazio no AWS Secrets Manager")
                return null
            }
            
            // Parse JSON para extrair signingKey
            val objectMapper = ObjectMapper()
            val secretMap = objectMapper.readValue(secretString, Map::class.java)
            val signingKey = secretMap["signingKey"] as? String
            
            if (signingKey.isNullOrBlank()) {
                logger.warn("Chave 'signingKey' não encontrada ou está vazia no secret")
                return null
            }
            
            logger.info("JWT secret obtido com sucesso do AWS Secrets Manager")
            return signingKey
            
        } catch (e: Exception) {
            logger.error("Erro ao buscar secret do AWS Secrets Manager: ${e.message}", e)
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