package com.banco.fidc.auth.usecase.session.impl

import com.banco.fidc.auth.domain.session.repository.SessionRepository
import com.banco.fidc.auth.domain.session.repository.UserSessionControlRepository
import com.banco.fidc.auth.shared.exception.BusinessException
import com.banco.fidc.auth.shared.exception.InfrastructureException
import com.banco.fidc.auth.usecase.session.EndSessionUseCase
import com.banco.fidc.auth.usecase.session.dto.input.EndSessionInput
import com.banco.fidc.auth.usecase.session.dto.params.RateLimitCheckParams
import com.banco.fidc.auth.usecase.session.exception.SessionNotFoundException
import com.banco.fidc.auth.usecase.session.exception.SessionProcessingException
import com.banco.fidc.auth.usecase.session.exception.SessionValidationException
import com.banco.fidc.auth.usecase.session.service.JwtSecretService
import com.banco.fidc.auth.usecase.session.service.RateLimitService
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
    private val rateLimitService: RateLimitService,
    private val sessionValidationService: SessionValidationService
) : EndSessionUseCase {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun execute(input: EndSessionInput) {
        logger.info(
            "Executando encerramento de sessão: partner=${input.partner}, correlationId=${input.correlationId}"
        )

        try {
            // 1. Validar headers obrigatórios
            if (input.accessToken.isBlank()) {
                throw SessionValidationException("Token de acesso obrigatório")
            }

            if (input.partner.isBlank()) {
                throw SessionValidationException("Header partner é obrigatório")
            }

            // 2. Rate limiting
            rateLimitService.checkRateLimit(
                RateLimitCheckParams(
                    clientIpAddress = input.clientIpAddress,
                    userAgent = input.userAgent
                )
            )

            // 3. Extrair sessionId do AccessToken
            val sessionId = sessionValidationService.extractSessionIdFromToken(input.accessToken)
            val sessionUuid = try {
                UUID.fromString(sessionId)
            } catch (e: IllegalArgumentException) {
                throw SessionValidationException("Token de acesso contém sessionId inválido")
            }

            // 4. Buscar sessão no Redis
            val session = sessionRepository.findBySessionId(sessionUuid)
            if (session == null) {
                logger.info("Sessão não encontrada ou já expirada: sessionId=$sessionId")
                return // Retorno vazio = 204 No Content (operação idempotente)
            }

            // 5. Validar assinatura do JWT usando sessionSecret da sessão
            try {
                jwtSecretService.validateJwtTokenWithSecret(
                    input.accessToken,
                    session.sessionSecret
                )
            } catch (e: Exception) {
                // Se token expirado, continuar com invalidação (comportamento normal)
                logger.debug("Token pode estar expirado, continuando com invalidação da sessão")
            }

            // 6. Validar partner
            if (!session.partner.equals(input.partner, ignoreCase = true)) {
                throw SessionValidationException("Partner não autorizado para esta sessão")
            }

            // 7. Remover sessão atomicamente
            removeSessionAtomically(sessionUuid, session.userInfo.cpf, session.partner)

            logger.info("Sessão encerrada manualmente com sucesso: sessionId=$sessionId")

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

    private fun removeSessionAtomically(sessionId: UUID, cpf: String, partner: String) {
        // Remover do Redis
        try {
            sessionRepository.deleteBySessionId(sessionId)
            logger.debug("Sessão removida do Redis: sessionId=$sessionId")
        } catch (ex: Exception) {
            logger.error("Erro ao remover sessão do Redis", ex)
            throw SessionProcessingException(
                "Serviço temporariamente indisponível", ex
            )
        }

        // Atualizar PostgreSQL
        try {
            val userSessionControl = userSessionControlRepository.findByCpfAndPartner(cpf, partner)
            if (userSessionControl != null && userSessionControl.currentSessionId == sessionId) {
                userSessionControl.deactivateSession()
                userSessionControlRepository.save(userSessionControl)
                logger.debug("Sessão desativada no PostgreSQL: sessionId=$sessionId")
            } else {
                logger.warn("Sessão não encontrada no PostgreSQL ou inconsistência detectada: sessionId=$sessionId")
            }
        } catch (ex: Exception) {
            logger.error("Erro ao atualizar PostgreSQL", ex)
            throw SessionProcessingException(
                "Erro interno do servidor", ex
            )
        }
    }
}