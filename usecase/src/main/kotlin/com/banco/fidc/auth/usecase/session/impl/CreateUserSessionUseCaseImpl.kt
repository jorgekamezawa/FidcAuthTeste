package com.banco.fidc.auth.usecase.session.impl

import com.banco.fidc.auth.domain.session.entity.Session
import com.banco.fidc.auth.domain.session.entity.SessionAccessHistory
import com.banco.fidc.auth.domain.session.entity.UserSessionControl
import com.banco.fidc.auth.domain.session.enum.SessionChannelEnum
import com.banco.fidc.auth.domain.session.repository.SessionRepository
import com.banco.fidc.auth.domain.session.repository.UserSessionControlRepository
import com.banco.fidc.auth.domain.session.repository.SessionAccessHistoryRepository
import com.banco.fidc.auth.shared.exception.*
import com.banco.fidc.auth.usecase.session.*
import com.banco.fidc.auth.usecase.session.configprovider.SessionConfigProvider
import com.banco.fidc.auth.usecase.session.dto.input.CreateUserSessionInput
import com.banco.fidc.auth.usecase.session.dto.output.CreateUserSessionOutput
import com.banco.fidc.auth.usecase.session.dto.params.*
import com.banco.fidc.auth.usecase.session.dto.result.*
import com.banco.fidc.auth.usecase.session.exception.*
import com.banco.fidc.auth.usecase.session.service.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class CreateUserSessionUseCaseImpl(
    private val sessionRepository: SessionRepository,
    private val userSessionControlRepository: UserSessionControlRepository,
    private val sessionAccessHistoryRepository: SessionAccessHistoryRepository,
    private val userManagementService: UserManagementService,
    private val fidcPermissionService: FidcPermissionService,
    private val jwtSecretService: JwtSecretService,
    private val rateLimitService: RateLimitService,
    private val sessionValidationService: SessionValidationService,
    private val cryptographyService: CryptographyService,
    private val sessionConfigProvider: SessionConfigProvider
) : CreateUserSessionUseCase {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun execute(input: CreateUserSessionInput): CreateUserSessionOutput {
        logger.info(
            "Executando criação de sessão: partner=${input.partner}, channel=${input.channel}, correlationId=${input.correlationId}"
        )
        
        try {
            // 1. Rate limiting
            rateLimitService.checkRateLimit(
                RateLimitCheckParams(
                    clientIpAddress = input.clientIpAddress,
                    userAgent = input.userAgent
                )
            )
            
            // 2. Validar e extrair CPF do JWT
            val cpf = sessionValidationService.extractCpfFromToken(input.signedData)
            
            // 3. Invalidar sessão anterior se existir
            invalidatePreviousSession(cpf, input.partner)
            
            // 4. Buscar dados do usuário
            val userManagementResult = userManagementService.getUser(
                UserManagementGetUserParams(
                    partner = input.partner,
                    cpf = cpf
                )
            )
            
            // 5. Buscar permissões gerais
            val permissionsResult = fidcPermissionService.getPermissions(
                FidcPermissionGetPermissionsParams(
                    partner = input.partner,
                    cpf = cpf,
                    relationshipId = null
                )
            )
            
            // 6. Gerar dados necessários para a sessão  
            val ttlMinutes = sessionConfigProvider.getTtlMinutes()
            val sessionSecret = cryptographyService.generateSecureSessionSecret()
            
            // 7. Criar entidades de domínio
            val sessionChannelEnum = SessionChannelEnum.valueOf(input.channel)
            
            val session = Session.create(
                partner = input.partner,
                userAgent = input.userAgent,
                channel = sessionChannelEnum,
                fingerprint = input.fingerprint,
                userInfo = com.banco.fidc.auth.domain.session.entity.UserInfo(
                    cpf = userManagementResult.userInfo.cpf,
                    fullName = userManagementResult.userInfo.fullName,
                    email = userManagementResult.userInfo.email,
                    birthDate = userManagementResult.userInfo.birthDate,
                    phoneNumber = userManagementResult.userInfo.phoneNumber
                ),
                fund = com.banco.fidc.auth.domain.session.entity.Fund(
                    id = userManagementResult.fund.id,
                    name = userManagementResult.fund.name,
                    type = userManagementResult.fund.type
                ),
                relationshipList = userManagementResult.relationshipList.map {
                    com.banco.fidc.auth.domain.session.entity.Relationship(
                        id = it.id,
                        type = it.type,
                        name = it.name,
                        status = it.status,
                        contractNumber = it.contractNumber
                    )
                },
                permissions = permissionsResult.permissions,
                ttlMinutes = ttlMinutes,
                sessionSecret = sessionSecret
            )
            
            // 8. Persistir sessão atomicamente
            persistSessionAtomically(session, input)
            
            // 9. Gerar access token usando dados da entidade
            val accessToken = jwtSecretService.generateAccessToken(
                sessionId = session.sessionId.toString(),
                sessionSecret = session.sessionSecret,
                expirationSeconds = (ttlMinutes * 60).toLong()
            )
            
            // 10. Preparar resposta
            logger.info("Sessão criada com sucesso: sessionId=${session.sessionId}")
            
            return CreateUserSessionOutput(
                userInfo = userManagementResult.userInfo.toUserInfoData(),
                fund = userManagementResult.fund.toFundData(),
                relationshipList = userManagementResult.relationshipList.map { it.toRelationshipData() },
                permissions = permissionsResult.permissions,
                accessToken = accessToken
            )
            
        } catch (ex: BusinessException) {
            logger.warn("Erro de negócio em criação de sessão: ${ex.message}")
            throw ex
        } catch (ex: InfrastructureException) {
            logger.error("Erro de infraestrutura [${ex.component}]: ${ex.message}", ex)
            throw ex
        } catch (ex: Exception) {
            logger.error("Erro inesperado em criação de sessão", ex)
            throw SessionProcessingException(
                "Erro ao processar criação de sessão", ex
            )
        }
    }
    
    private fun invalidatePreviousSession(cpf: String, partner: String) {
        try {
            // Buscar sessão anterior diretamente no Redis usando o índice de CPF
            val existingSession = sessionRepository.findByCpfAndPartner(cpf, partner)
            
            if (existingSession != null) {
                logger.info("Invalidando sessão anterior encontrada no Redis: sessionId=${existingSession.sessionId}")
                
                // Remover sessão do Redis (que também limpa o índice)
                sessionRepository.deleteBySessionId(existingSession.sessionId)
                logger.debug("Sessão anterior removida do Redis com sucesso")
                
                // Atualizar controle de sessão no banco para marcar como inativa
                val existingControl = userSessionControlRepository.findByCpfAndPartner(cpf, partner)
                existingControl?.let { control ->
                    if (control.isActive && control.currentSessionId == existingSession.sessionId) {
                        control.deactivateSession()
                        userSessionControlRepository.save(control)
                        logger.debug("Controle de sessão atualizado no banco")
                    }
                }
            } else {
                logger.debug("Nenhuma sessão anterior encontrada no Redis para cpf=${cpf.take(3)}***, partner=$partner")
            }
            
        } catch (ex: Exception) {
            logger.error("Erro ao invalidar sessão anterior", ex)
            throw SessionProcessingException(
                "Erro ao invalidar sessão anterior", ex
            )
        }
    }
    
    private fun persistSessionAtomically(session: Session, input: CreateUserSessionInput) {
        // Atualizar ou criar controle de sessão
        val sessionControl = userSessionControlRepository
            .findByCpfAndPartner(session.userInfo.cpf, session.partner)
            ?.apply {
                startNewSession(session.sessionId)
            } ?: UserSessionControl.createNew(
                cpf = session.userInfo.cpf,
                partner = session.partner
            ).apply {
                startNewSession(session.sessionId)
            }
        
        val savedSessionControl = userSessionControlRepository.save(sessionControl)
        
        // Criar histórico de acesso
        val ipAddress = try {
            java.net.InetAddress.getByName(input.clientIpAddress)
        } catch (ex: Exception) {
            logger.warn("Erro ao converter IP address: ${input.clientIpAddress}", ex)
            null
        }
        
        val accessHistory = SessionAccessHistory.createNew(
            userSessionControlId = savedSessionControl.id,
            sessionId = session.sessionId,
            ipAddress = ipAddress,
            userAgent = input.userAgent,
            latitude = input.latitude?.toDoubleOrNull(),
            longitude = input.longitude?.toDoubleOrNull(),
            locationAccuracy = input.locationAccuracy?.toIntOrNull(),
            locationTimestamp = input.locationTimestamp?.let { 
                try {
                    java.time.LocalDateTime.parse(it)
                } catch (e: Exception) {
                    logger.warn("Error parsing locationTimestamp: $it", e)
                    null
                }
            }
        )
        
        sessionAccessHistoryRepository.save(accessHistory)
        
        // Salvar sessão no cache
        sessionRepository.save(session)
        
        logger.debug("Sessão persistida atomicamente: sessionId=${session.sessionId}")
    }
}