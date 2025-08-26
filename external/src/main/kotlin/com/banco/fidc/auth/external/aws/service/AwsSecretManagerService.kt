package com.banco.fidc.auth.external.aws.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

@Service
class AwsSecretManagerService(
    private val secretsManagerClient: SecretsManagerClient,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun getSecretAsString(secretName: String): String? {
        return try {
            logger.debug("Buscando secret como string: $secretName")
            
            val request = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build()
                
            val response = secretsManagerClient.getSecretValue(request)
            val secretString = response.secretString()
            
            if (secretString.isNullOrBlank()) {
                logger.warn("Secret string está vazio: $secretName")
                return null
            }
            
            logger.debug("Secret obtido com sucesso: $secretName")
            secretString
            
        } catch (e: Exception) {
            logger.error("Erro ao buscar secret '$secretName': ${e.message}", e)
            null
        }
    }

    fun getSecretAsMap(secretName: String): Map<String, Any>? {
        return try {
            val secretString = getSecretAsString(secretName) ?: return null
            
            logger.debug("Convertendo secret para Map: $secretName")
            @Suppress("UNCHECKED_CAST")
            val secretMap = objectMapper.readValue(secretString, Map::class.java) as Map<String, Any>
            
            logger.debug("Secret convertido para Map com sucesso: $secretName")
            secretMap
            
        } catch (e: Exception) {
            logger.error("Erro ao converter secret '$secretName' para Map: ${e.message}", e)
            null
        }
    }

    fun <T> getSecretAs(secretName: String, clazz: Class<T>): T? {
        return try {
            val secretString = getSecretAsString(secretName) ?: return null
            
            logger.debug("Convertendo secret para ${clazz.simpleName}: $secretName")
            val secretObject = objectMapper.readValue(secretString, clazz)
            
            logger.debug("Secret convertido para ${clazz.simpleName} com sucesso: $secretName")
            secretObject
            
        } catch (e: Exception) {
            logger.error("Erro ao converter secret '$secretName' para ${clazz.simpleName}: ${e.message}", e)
            null
        }
    }
}