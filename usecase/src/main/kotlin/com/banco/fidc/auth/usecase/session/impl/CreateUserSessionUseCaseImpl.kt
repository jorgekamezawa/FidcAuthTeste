package com.banco.fidc.auth.usecase.session.impl

import com.banco.fidc.auth.domain.session.entity.*
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
import com.banco.fidc.auth.usecase.session.dto.common.*
import java.util.UUID
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
        logger.info("Executando criação de sessão: partner=${input.partner}, channel=${input.channel}")
        
        try {
            checkRateLimit(input)
            val cpf = extractCpfFromSignedData(input.signedData)
            invalidatePreviousSession(cpf, input.partner)
            
            val userManagementResult = fetchUserData(input.partner, cpf)
            val permissionsResult = fetchUserPermissions(input.partner, cpf)
            
            val session = createSessionEntity(input, userManagementResult, permissionsResult)
            persistSessionAtomically(session, input)
            
            val accessToken = generateAccessToken(session)
            
            logger.info("Sessão criada com sucesso: sessionId=${session.sessionId}")
            return buildSessionOutput(userManagementResult, permissionsResult, accessToken)
            
        } catch (ex: InvalidSessionEnumException) {
            logger.warn("Channel inválido recebido na criação de sessão - Enum: SessionChannelEnum, Valor: '${input.channel}'")
            throw InvalidInputException("Channel '${input.channel}' é incorreto. Valores aceitos: ${SessionChannelEnum.getAcceptedValues()}")
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
    
    private fun checkRateLimit(input: CreateUserSessionInput) {
        rateLimitService.checkRateLimit(
            RateLimitCheckParams(
                clientIpAddress = input.clientIpAddress,
                userAgent = input.userAgent
            )
        )
    }
    
    private fun extractCpfFromSignedData(signedData: String): String {
        return sessionValidationService.extractCpfFromToken(signedData)
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
    
    private fun fetchUserData(partner: String, cpf: String): UserManagementGetUserResult {
        return userManagementService.getUser(
            UserManagementGetUserParams(
                partner = partner,
                cpf = cpf
            )
        )
    }
    
    private fun fetchUserPermissions(partner: String, cpf: String): FidcPermissionGetPermissionsResult {
        return fidcPermissionService.getPermissions(
            FidcPermissionGetPermissionsParams(
                partner = partner,
                cpf = cpf,
                relationshipId = null
            )
        )
    }
    
    private fun createSessionEntity(
        input: CreateUserSessionInput,
        userManagementResult: UserManagementGetUserResult,
        permissionsResult: FidcPermissionGetPermissionsResult
    ): Session {
        val ttlMinutes = sessionConfigProvider.getTtlMinutes()
        val sessionSecret = cryptographyService.generateSecureSessionSecret()
        val sessionChannelEnum = SessionChannelEnum.fromValue(input.channel)
        
        return Session.create(
            partner = input.partner,
            userAgent = input.userAgent,
            channel = sessionChannelEnum,
            fingerprint = input.fingerprint,
            userInfo = mapToUserInfoEntity(userManagementResult.userInfo),
            fund = mapToFundEntity(userManagementResult.fund),
            relationshipList = mapToRelationshipEntities(userManagementResult.relationshipList),
            permissions = permissionsResult.permissions,
            ttlMinutes = ttlMinutes,
            sessionSecret = sessionSecret
        )
    }
    
    private fun mapToUserInfoEntity(userInfo: UserInfoResult) = 
        UserInfo(
            cpf = userInfo.cpf,
            fullName = userInfo.fullName,
            email = userInfo.email,
            birthDate = userInfo.birthDate,
            phoneNumber = userInfo.phoneNumber
        )
    
    private fun mapToFundEntity(fund: FundResult) = 
        Fund(
            id = fund.id,
            name = fund.name,
            type = fund.type
        )
    
    private fun mapToRelationshipEntities(relationships: List<RelationshipResult>) = 
        relationships.map {
            Relationship(
                id = it.id,
                type = it.type,
                name = it.name,
                status = it.status,
                contractNumber = it.contractNumber
            )
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
    
    private fun generateAccessToken(session: Session): String {
        return jwtSecretService.generateAccessToken(
            sessionId = session.sessionId.toString(),
            sessionSecret = session.sessionSecret,
            expirationSeconds = (session.ttlMinutes * 60).toLong()
        )
    }
    
    private fun buildSessionOutput(
        userManagementResult: UserManagementGetUserResult,
        permissionsResult: FidcPermissionGetPermissionsResult,
        accessToken: String
    ): CreateUserSessionOutput {
        return CreateUserSessionOutput(
            userInfo = userManagementResult.userInfo.toUserInfoData(),
            fund = userManagementResult.fund.toFundData(),
            relationshipList = userManagementResult.relationshipList.map { it.toRelationshipData() },
            permissions = permissionsResult.permissions,
            accessToken = accessToken
        )
    }
}