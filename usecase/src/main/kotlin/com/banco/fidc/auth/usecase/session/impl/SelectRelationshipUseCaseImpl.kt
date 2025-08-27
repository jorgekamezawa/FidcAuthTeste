package com.banco.fidc.auth.usecase.session.impl

import com.banco.fidc.auth.domain.session.entity.Relationship
import com.banco.fidc.auth.domain.session.entity.Session
import com.banco.fidc.auth.domain.session.repository.SessionRepository
import com.banco.fidc.auth.shared.exception.BusinessException
import com.banco.fidc.auth.shared.exception.InfrastructureException
import com.banco.fidc.auth.shared.exception.InvalidSessionEnumException
import com.banco.fidc.auth.usecase.session.SelectRelationshipUseCase
import com.banco.fidc.auth.usecase.session.dto.input.SelectRelationshipInput
import com.banco.fidc.auth.usecase.session.dto.output.SelectRelationshipOutput
import com.banco.fidc.auth.usecase.session.dto.output.toSelectRelationshipOutput
import com.banco.fidc.auth.usecase.session.dto.params.FidcPermissionGetPermissionsParams
import com.banco.fidc.auth.usecase.session.dto.params.RateLimitCheckParams
import com.banco.fidc.auth.usecase.session.dto.result.FidcPermissionGetPermissionsResult
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
        logger.info("Executando seleção de relacionamento: relationshipId=${input.relationshipId}, partner=${input.partner}")

        try {
            validateInput(input)
            checkRateLimit(input)
            
            val sessionId = extractSessionIdFromToken(input.accessToken)
            val session = findAndValidateSession(sessionId, input.partner)
            validateAccessToken(input.accessToken, session)
            
            val selectedRelationship = findAndValidateRelationship(session, input.relationshipId)
            val permissionsResult = fetchRelationshipPermissions(session, input.relationshipId)
            
            updateSessionWithRelationship(session, selectedRelationship, permissionsResult)
            persistUpdatedSession(session)
            
            logger.info("Relacionamento selecionado com sucesso: sessionId=${session.sessionId}, relationshipId=${input.relationshipId}")
            return buildOutput(session, input.accessToken)
            
        } catch (ex: InvalidSessionEnumException) {
            logger.error("Dados corrompidos encontrados no Redis durante seleção de relacionamento - Enum: ${ex.message}")
            throw SessionProcessingException("Erro interno do servidor - dados de sessão inconsistentes")
        } catch (ex: BusinessException) {
            logger.warn("Erro de negócio em seleção de relacionamento: ${ex.message}")
            throw ex
        } catch (ex: InfrastructureException) {
            logger.error("Erro de infraestrutura [${ex.component}]: ${ex.message}", ex)
            throw ex
        } catch (ex: Exception) {
            logger.error("Erro inesperado em seleção de relacionamento", ex)
            throw SessionProcessingException("Erro ao processar seleção de relacionamento", ex)
        }
    }
    
    private fun validateInput(input: SelectRelationshipInput) {
        if (input.partner.isBlank()) {
            throw SessionValidationException("Header partner é obrigatório")
        }
    }
    
    private fun checkRateLimit(input: SelectRelationshipInput) {
        rateLimitService.checkRateLimit(
            RateLimitCheckParams(
                clientIpAddress = input.clientIpAddress,
                userAgent = input.userAgent
            )
        )
    }
    
    private fun extractSessionIdFromToken(accessToken: String): UUID {
        val sessionId = sessionValidationService.extractSessionIdFromToken(accessToken)
        return try {
            UUID.fromString(sessionId)
        } catch (e: IllegalArgumentException) {
            throw SessionValidationException("Token de acesso contém sessionId inválido")
        }
    }
    
    private fun findAndValidateSession(sessionId: UUID, partner: String): Session {
        val session = sessionRepository.findBySessionId(sessionId)
            ?: throw SessionNotFoundException("Sessão não encontrada ou expirada")
            
        if (!session.partner.equals(partner, ignoreCase = true)) {
            throw SessionValidationException("Partner não autorizado para esta sessão")
        }
        
        return session
    }
    
    private fun validateAccessToken(accessToken: String, session: Session) {
        jwtSecretService.validateJwtTokenWithSecret(accessToken, session.sessionSecret)
    }
    
    private fun findAndValidateRelationship(session: Session, relationshipId: String): Relationship {
        val selectedRelationship = session.relationshipList.find { it.id == relationshipId }
            ?: throw SessionValidationException("Relacionamento não encontrado na sessão")
        
        if (selectedRelationship.status != "ACTIVE") {
            throw SessionValidationException("Relacionamento inativo")
        }
        
        return selectedRelationship
    }
    
    private fun fetchRelationshipPermissions(session: Session, relationshipId: String): FidcPermissionGetPermissionsResult {
        return fidcPermissionService.getPermissions(
            FidcPermissionGetPermissionsParams(
                partner = session.partner,
                cpf = session.userInfo.cpf,
                relationshipId = relationshipId
            )
        )
    }
    
    private fun updateSessionWithRelationship(
        session: Session, 
        selectedRelationship: Relationship, 
        permissionsResult: FidcPermissionGetPermissionsResult
    ) {
        session.selectRelationship(selectedRelationship)
        session.updatePermissions(permissionsResult.permissions)
    }
    
    private fun persistUpdatedSession(session: Session) {
        sessionRepository.update(session)
    }
    
    private fun buildOutput(session: Session, accessToken: String): SelectRelationshipOutput {
        return session.toSelectRelationshipOutput(accessToken)
    }
}