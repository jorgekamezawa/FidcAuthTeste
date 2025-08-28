package com.banco.fidc.auth.usecase.session.impl

import com.banco.fidc.auth.domain.session.entity.Session
import com.banco.fidc.auth.domain.session.repository.SessionRepository
import com.banco.fidc.auth.domain.session.repository.UserSessionControlRepository
import com.banco.fidc.auth.shared.exception.BusinessException
import com.banco.fidc.auth.shared.exception.InfrastructureException
import com.banco.fidc.auth.shared.exception.InvalidSessionEnumException
import com.banco.fidc.auth.usecase.session.EndSessionUseCase
import com.banco.fidc.auth.usecase.session.dto.input.EndSessionInput
import com.banco.fidc.auth.usecase.session.exception.SessionNotFoundException
import com.banco.fidc.auth.usecase.session.exception.SessionProcessingException
import com.banco.fidc.auth.usecase.session.exception.SessionValidationException
import com.banco.fidc.auth.usecase.session.service.JwtSecretService
import com.banco.fidc.auth.usecase.session.service.SessionValidationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class EndSessionUseCaseImpl(
    private val sessionRepository: SessionRepository,
    private val userSessionControlRepository: UserSessionControlRepository,
    private val jwtSecretService: JwtSecretService,
    private val sessionValidationService: SessionValidationService
) : EndSessionUseCase {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun execute(input: EndSessionInput) {
        logger.info("Executando encerramento de sessão: partner=${input.partner}")

        try {
            validateInput(input)
            
            val sessionId = extractSessionIdFromToken(input.accessToken)
            val session = findSession(sessionId)
            
            if (session == null) {
                logger.info("Sessão não encontrada no Redis: sessionId=${sessionId}")
                handleSessionNotFoundInRedis(sessionId, input.partner)
                return
            }
            
            validateSessionPartner(session, input.partner)
            validateAccessTokenSafely(input.accessToken, session.sessionSecret)
            
            removeSessionAtomically(sessionId, session.userInfo.cpf, session.partner)
            
            logger.info("Sessão encerrada manualmente com sucesso: sessionId=${sessionId}")

        } catch (ex: InvalidSessionEnumException) {
            logger.error("Dados corrompidos encontrados no Redis durante encerramento de sessão - Enum: ${ex.message}")
            throw SessionProcessingException("Erro interno do servidor - dados de sessão inconsistentes")
        } catch (ex: BusinessException) {
            logger.warn("Erro de negócio em encerramento de sessão: ${ex.message}")
            throw ex
        } catch (ex: InfrastructureException) {
            logger.error("Erro de infraestrutura [${ex.component}]: ${ex.message}", ex)
            throw ex
        } catch (ex: Exception) {
            logger.error("Erro inesperado em encerramento de sessão", ex)
            throw SessionProcessingException(
                "Erro ao processar encerramento de sessão", ex
            )
        }
    }

    private fun validateInput(input: EndSessionInput) {
        if (input.accessToken.isBlank()) {
            throw SessionValidationException("Token de acesso obrigatório")
        }
        
        if (input.partner.isBlank()) {
            throw SessionValidationException("Header partner é obrigatório")
        }
    }
    
    
    private fun extractSessionIdFromToken(accessToken: String): UUID {
        val sessionId = sessionValidationService.extractSessionIdFromToken(accessToken)
        return try {
            UUID.fromString(sessionId)
        } catch (e: IllegalArgumentException) {
            throw SessionValidationException("Token de acesso contém sessionId inválido")
        }
    }
    
    private fun findSession(sessionId: UUID) = sessionRepository.findBySessionId(sessionId)
    
    private fun handleSessionNotFoundInRedis(sessionId: UUID, partner: String) {
        try {
            // Extrair CPF do token seria complexo sem a sessão do Redis
            // Então vamos buscar por sessionId diretamente no PostgreSQL
            val userSessionControl = userSessionControlRepository.findByCurrentSessionId(sessionId)
            
            if (userSessionControl != null) {
                // Verificar se o partner coincide
                if (!userSessionControl.partner.equals(partner, ignoreCase = true)) {
                    throw SessionValidationException("Partner não autorizado para esta sessão")
                }
                
                // Se já está inativa, operação idempotente
                if (!userSessionControl.isActive) {
                    logger.debug("Sessão já estava inativa no PostgreSQL: sessionId=${sessionId}")
                    return
                }
                
                // Se está ativa, desativar
                userSessionControl.deactivateSession()
                userSessionControlRepository.save(userSessionControl)
                logger.info("Sessão desativada no PostgreSQL: sessionId=${sessionId}")
            } else {
                logger.debug("Sessão não encontrada nem no Redis nem no PostgreSQL: sessionId=${sessionId}")
            }
        } catch (ex: BusinessException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Erro ao verificar sessão no PostgreSQL", ex)
            throw SessionProcessingException("Erro interno do servidor", ex)
        }
    }
    
    private fun validateSessionPartner(session: com.banco.fidc.auth.domain.session.entity.Session, partner: String) {
        if (!session.partner.equals(partner, ignoreCase = true)) {
            throw SessionValidationException("Partner não autorizado para esta sessão")
        }
    }
    
    private fun validateAccessTokenSafely(accessToken: String, sessionSecret: String) {
        try {
            jwtSecretService.validateJwtTokenWithSecret(accessToken, sessionSecret)
        } catch (e: Exception) {
            logger.debug("Token pode estar expirado, continuando com invalidação da sessão")
        }
    }
    
    private fun removeSessionAtomically(sessionId: UUID, cpf: String, partner: String) {
        removeSessionFromRedis(sessionId)
        updatePostgreSQLSession(sessionId, cpf, partner)
    }
    
    private fun removeSessionFromRedis(sessionId: UUID) {
        try {
            sessionRepository.deleteBySessionId(sessionId)
            logger.debug("Sessão removida do Redis: sessionId=${sessionId}")
        } catch (ex: Exception) {
            logger.error("Erro ao remover sessão do Redis", ex)
            throw SessionProcessingException("Serviço temporariamente indisponível", ex)
        }
    }
    
    private fun updatePostgreSQLSession(sessionId: UUID, cpf: String, partner: String) {
        try {
            val userSessionControl = userSessionControlRepository.findByCpfAndPartner(cpf, partner)
            if (userSessionControl != null && userSessionControl.currentSessionId == sessionId) {
                userSessionControl.deactivateSession()
                userSessionControlRepository.save(userSessionControl)
                logger.debug("Sessão desativada no PostgreSQL: sessionId=${sessionId}")
            } else {
                logger.warn("Sessão não encontrada no PostgreSQL ou inconsistência detectada: sessionId=${sessionId}")
            }
        } catch (ex: Exception) {
            logger.error("Erro ao atualizar PostgreSQL", ex)
            throw SessionProcessingException("Erro interno do servidor", ex)
        }
    }
}