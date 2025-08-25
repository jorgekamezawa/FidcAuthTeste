package com.banco.fidc.auth.usecase.session.impl

import com.banco.fidc.auth.domain.session.repository.SessionRepository
import com.banco.fidc.auth.shared.exception.BusinessException
import com.banco.fidc.auth.shared.exception.InfrastructureException
import com.banco.fidc.auth.usecase.session.SelectRelationshipUseCase
import com.banco.fidc.auth.usecase.session.dto.input.SelectRelationshipInput
import com.banco.fidc.auth.usecase.session.dto.output.SelectRelationshipOutput
import com.banco.fidc.auth.usecase.session.dto.output.toSelectRelationshipOutput
import com.banco.fidc.auth.usecase.session.dto.params.FidcPermissionGetPermissionsParams
import com.banco.fidc.auth.usecase.session.dto.params.RateLimitCheckParams
import com.banco.fidc.auth.usecase.session.exception.SessionNotFoundException
import com.banco.fidc.auth.usecase.session.exception.SessionProcessingException
import com.banco.fidc.auth.usecase.session.exception.SessionValidationException
import com.banco.fidc.auth.usecase.session.service.FidcPermissionService
import com.banco.fidc.auth.usecase.session.service.JwtSecretService
import com.banco.fidc.auth.usecase.session.service.RateLimitService
import com.banco.fidc.auth.usecase.session.service.SessionValidationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class SelectRelationshipUseCaseImpl(
    private val sessionRepository: SessionRepository,
    private val fidcPermissionService: FidcPermissionService,
    private val jwtSecretService: JwtSecretService,
    private val rateLimitService: RateLimitService,
    private val sessionValidationService: SessionValidationService
) : SelectRelationshipUseCase {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun execute(input: SelectRelationshipInput): SelectRelationshipOutput {
        logger.info(
            "Executando seleção de relacionamento: relationshipId=${input.relationshipId}, correlationId=${input.correlationId}"
        )

        try {
            // 1. Rate limiting
            rateLimitService.checkRateLimit(
                RateLimitCheckParams(
                    clientIpAddress = input.clientIpAddress,
                    userAgent = input.userAgent
                )
            )

            // 2. Extrair sessionId do AccessToken
            val sessionId = sessionValidationService.extractSessionIdFromToken(input.accessToken)
            val sessionUuid = try {
                UUID.fromString(sessionId)
            } catch (e: IllegalArgumentException) {
                throw SessionValidationException("Token de acesso inválido")
            }

            // 3. Buscar sessão no Redis
            val session = sessionRepository.findBySessionId(sessionUuid)
                ?: throw SessionNotFoundException("Sessão não encontrada ou expirada")

            // 4. Validar assinatura do JWT usando sessionSecret da sessão
            val jwtClaims = jwtSecretService.validateJwtTokenWithSecret(
                input.accessToken,
                session.sessionSecret
            )

            // 5. Verificar expiração do token
            sessionValidationService.validateTokenExpiration(jwtClaims)

            // 6. Validar relacionamento
            sessionValidationService.validateRelationshipExists(session, input.relationshipId)

            // 7. Buscar relacionamento específico
            val selectedRelationship = session.relationshipList.find { it.id == input.relationshipId }!!

            // 8. Buscar permissões específicas do relacionamento
            val permissionsResult = fidcPermissionService.getPermissions(
                FidcPermissionGetPermissionsParams(
                    partner = session.partner,
                    cpf = session.userInfo.cpf,
                    relationshipId = input.relationshipId
                )
            )

            // 9. Atualizar sessão
            session.selectRelationship(selectedRelationship)
            session.updatePermissions(permissionsResult.permissions)

            // 10. Atualizar sessão no Redis (preserva TTL)
            sessionRepository.update(session)

            logger.info("Relacionamento selecionado com sucesso: sessionId=${session.sessionId}, relationshipId=${input.relationshipId}")

            // 11. Reutilizar o AccessToken original (já validado)
            return session.toSelectRelationshipOutput(input.accessToken)

        } catch (ex: BusinessException) {
            logger.warn("Erro de negócio em seleção de relacionamento: ${ex.message}")
            throw ex
        } catch (ex: InfrastructureException) {
            logger.error("Erro de infraestrutura [${ex.component}]: ${ex.message}", ex)
            throw ex
        } catch (ex: Exception) {
            logger.error("Erro inesperado em seleção de relacionamento", ex)
            throw SessionProcessingException(
                "Erro ao processar seleção de relacionamento", ex
            )
        }
    }
}