package com.banco.fidc.auth.external.jwt.service

import com.banco.fidc.auth.external.aws.service.AwsSecretManagerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class JwtSecretResolver(
    private val awsSecretManagerService: AwsSecretManagerService,
    @Value("\${aws.secret-manager.jwt.name}")
    private val jwtSecretName: String,
    @Value("\${jwt.fallback-secret}")
    private val fallbackSecret: String
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun getJwtSigningKey(): String {
        logger.debug("Buscando JWT signing key")
        
        // 1. Tentar buscar da AWS
        val awsSecret = tryGetFromAwsSecrets()
        if (awsSecret != null) {
            return awsSecret
        }
        
        // 2. Tentar buscar do FidcPassword API
        val fidcPasswordSecret = tryGetFromFidcPasswordApi()
        if (fidcPasswordSecret != null) {
            return fidcPasswordSecret
        }
        
        // 3. Usar fallback
        logger.warn("Usando fallback secret para JWT")
        return fallbackSecret
    }

    private fun tryGetFromAwsSecrets(): String? {
        return try {
            logger.debug("Tentando buscar JWT secret do AWS Secrets Manager: $jwtSecretName")
            
            val secretMap = awsSecretManagerService.getSecretAsMap(jwtSecretName)
                ?: return null
            
            val signingKey = secretMap["signingKey"] as? String
            
            if (signingKey.isNullOrBlank()) {
                logger.warn("Chave 'signingKey' não encontrada ou está vazia no secret: $jwtSecretName")
                return null
            }
            
            logger.info("JWT secret obtido com sucesso do AWS Secrets Manager")
            signingKey
            
        } catch (e: Exception) {
            logger.error("Erro ao buscar JWT secret do AWS: ${e.message}", e)
            null
        }
    }
    
    private fun tryGetFromFidcPasswordApi(): String? {
        return try {
            logger.debug("Tentando buscar JWT secret do FidcPassword API...")
            // TODO: Implementar chamada para FidcPassword API
            // fidcPasswordClient.getJwtSecret()
            null
        } catch (e: Exception) {
            logger.warn("Erro ao buscar secret do FidcPassword: ${e.message}")
            null
        }
    }
}