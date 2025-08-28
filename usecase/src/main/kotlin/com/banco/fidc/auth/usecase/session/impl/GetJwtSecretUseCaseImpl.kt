package com.banco.fidc.auth.usecase.session.impl

import com.banco.fidc.auth.shared.exception.BusinessException
import com.banco.fidc.auth.shared.exception.InfrastructureException
import com.banco.fidc.auth.usecase.session.GetJwtSecretUseCase
import com.banco.fidc.auth.usecase.session.dto.input.GetJwtSecretInput
import com.banco.fidc.auth.usecase.session.dto.output.GetJwtSecretOutput
import com.banco.fidc.auth.usecase.session.exception.SessionProcessingException
import com.banco.fidc.auth.usecase.session.service.JwtSecretService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GetJwtSecretUseCaseImpl(
    private val jwtSecretService: JwtSecretService
) : GetJwtSecretUseCase {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun execute(input: GetJwtSecretInput): GetJwtSecretOutput {
        logger.info("Executando obtenção de JWT secret")
        
        try {
            val jwtSecret = jwtSecretService.getSecretHash()
            
            logger.info("JWT secret obtida com sucesso")
            return GetJwtSecretOutput(secret = jwtSecret)
            
        } catch (e: BusinessException) {
            logger.error("Erro de negócio ao obter JWT secret: ${e.message}", e)
            throw e
        } catch (e: InfrastructureException) {
            logger.error("Erro de infraestrutura ao obter JWT secret: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Erro inesperado ao obter JWT secret: ${e.message}", e)
            throw SessionProcessingException("Erro interno do servidor", e)
        }
    }
}