package com.banco.fidc.auth.external.usermanagement.impl

import com.banco.fidc.auth.external.usermanagement.client.UserManagementFeignClient
import com.banco.fidc.auth.external.usermanagement.dto.response.*
import com.banco.fidc.auth.external.usermanagement.exception.UserManagementException
import com.banco.fidc.auth.external.config.feign.FeignCallHelper
import com.banco.fidc.auth.usecase.session.service.UserManagementService
import com.banco.fidc.auth.usecase.session.dto.params.UserManagementGetUserParams
import com.banco.fidc.auth.usecase.session.dto.result.UserManagementGetUserResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UserManagementServiceImpl(
    private val feignClient: UserManagementFeignClient,
    private val feignCallHelper: FeignCallHelper
) : UserManagementService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getUser(params: UserManagementGetUserParams): UserManagementGetUserResult {
        logger.debug("Getting user from UserManagement: partner=${params.partner}, cpf=${params.cpf.take(3)}***")

        // TODO: QUANDO A API ESTIVER PRONTA, DESCOMENTE A LINHA ABAIXO E COMENTE O MÉTODO MOCKADO
        // return callRealApi(params)
        
        // MOCK: REMOVER QUANDO A API ESTIVER PRONTA
        return getMockUserData(params)
    }
    
    // MÉTODO REAL PARA QUANDO A API ESTIVER PRONTA
    private fun callRealApi(params: UserManagementGetUserParams): UserManagementGetUserResult {
        val response = feignCallHelper.executeFeignCall(
            componentName = "UserManagement",
            operation = "getUser",
            call = { feignClient.getUser(params.partner, params.cpf) },
            createException = { msg, cause -> UserManagementException(msg, cause) },
            handle404AsNull = false
        ) ?: throw UserManagementException("Null response from UserManagement API")

        return response.toResult()
    }

    // MOCK: 3 EXEMPLOS DIFERENTES - REMOVER QUANDO A API ESTIVER PRONTA
    private fun getMockUserData(params: UserManagementGetUserParams): UserManagementGetUserResult {
        logger.warn("USANDO DADOS MOCKADOS - UserManagement API não disponível ainda")
        
        return when {
            params.cpf.endsWith("1") -> createMockUser1(params)
            params.cpf.endsWith("2") -> createMockUser2(params)
            else -> createMockUser3(params)
        }
    }
    
    private fun createMockUser1(params: UserManagementGetUserParams): UserManagementGetUserResult {
        return UserManagementGetUserResult(
            userInfo = com.banco.fidc.auth.usecase.session.dto.result.UserInfoResult(
                cpf = params.cpf,
                fullName = "João Silva Santos",
                email = "joao.silva@email.com",
                birthDate = LocalDate.of(1985, 3, 15),
                phoneNumber = "+5511999887766"
            ),
            fund = com.banco.fidc.auth.usecase.session.dto.result.FundResult(
                id = "CRED001",
                name = "Prevcom RS",
                type = "PREVIDENCIA"
            ),
            relationshipList = listOf(
                com.banco.fidc.auth.usecase.session.dto.result.RelationshipResult(
                    id = "REL001",
                    type = "PLANO_PREVIDENCIA",
                    name = "Plano Previdência Básico",
                    status = "ACTIVE",
                    contractNumber = "378192372163682"
                ),
                com.banco.fidc.auth.usecase.session.dto.result.RelationshipResult(
                    id = "REL002",
                    type = "PLANO_PREVIDENCIA", 
                    name = "Plano Previdência Premium",
                    status = "ACTIVE",
                    contractNumber = "4353453456475465"
                )
            )
        )
    }
    
    private fun createMockUser2(params: UserManagementGetUserParams): UserManagementGetUserResult {
        return UserManagementGetUserResult(
            userInfo = com.banco.fidc.auth.usecase.session.dto.result.UserInfoResult(
                cpf = params.cpf,
                fullName = "Maria Oliveira Costa",
                email = "maria.costa@empresa.com",
                birthDate = LocalDate.of(1990, 7, 22),
                phoneNumber = "+5511888776655"
            ),
            fund = com.banco.fidc.auth.usecase.session.dto.result.FundResult(
                id = "CRED002",
                name = "CAIO Investimentos",
                type = "INVESTIMENTO"
            ),
            relationshipList = listOf(
                com.banco.fidc.auth.usecase.session.dto.result.RelationshipResult(
                    id = "REL003",
                    type = "CONTA_INVESTIMENTO",
                    name = "Conta Investimentos Master",
                    status = "ACTIVE",
                    contractNumber = "567890123456789"
                )
            )
        )
    }
    
    private fun createMockUser3(params: UserManagementGetUserParams): UserManagementGetUserResult {
        return UserManagementGetUserResult(
            userInfo = com.banco.fidc.auth.usecase.session.dto.result.UserInfoResult(
                cpf = params.cpf,
                fullName = "Carlos Eduardo Lima",
                email = "carlos.lima@teste.com",
                birthDate = LocalDate.of(1978, 12, 5),
                phoneNumber = "+5511777665544"
            ),
            fund = com.banco.fidc.auth.usecase.session.dto.result.FundResult(
                id = "CRED003",
                name = "Fundo Misto",
                type = "MISTO"
            ),
            relationshipList = listOf(
                com.banco.fidc.auth.usecase.session.dto.result.RelationshipResult(
                    id = "REL004",
                    type = "CONTA_CORRENTE",
                    name = "Conta Empresarial",
                    status = "ACTIVE", 
                    contractNumber = "789012345678901"
                ),
                com.banco.fidc.auth.usecase.session.dto.result.RelationshipResult(
                    id = "REL005",
                    type = "CONTA_POUPANCA",
                    name = "Poupança Gold",
                    status = "ACTIVE",
                    contractNumber = "890123456789012"
                ),
                com.banco.fidc.auth.usecase.session.dto.result.RelationshipResult(
                    id = "REL006",
                    type = "FINANCIAMENTO",
                    name = "Financiamento Imobiliário",
                    status = "SUSPENDED",
                    contractNumber = "901234567890123"
                )
            )
        )
    }
}