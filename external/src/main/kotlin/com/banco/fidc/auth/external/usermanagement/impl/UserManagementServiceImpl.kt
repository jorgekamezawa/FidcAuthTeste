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

    // MOCK: BASEADO EM PARTNER E CPF - REMOVER QUANDO A API ESTIVER PRONTA
    // 
    // REGRAS DO MOCK:
    // 1. Partners suportados: "prevcom" e "caio" (case-insensitive)
    // 2. CPF terminando em 4 ou 5: simula usuário não encontrado
    // 3. Qualquer outro partner: simula partner não encontrado
    // 4. CPF com outros finais + partner válido: retorna dados mockados específicos
    //
    // EXEMPLOS:
    // - getUser(cpf="12345678901", partner="prevcom") → dados PREVCOM ✅
    // - getUser(cpf="12345678902", partner="caio") → dados CAIO ✅
    // - getUser(cpf="12345678904", partner="prevcom") → UserManagementException ❌
    // - getUser(cpf="12345678901", partner="itau") → UserManagementException ❌
    //
    private fun getMockUserData(params: UserManagementGetUserParams): UserManagementGetUserResult {
        logger.warn("USANDO DADOS MOCKADOS - UserManagement API não disponível ainda")
        
        // Regra 1: CPF terminando em 4 ou 5 = usuário não encontrado (simula casos de erro)
        if (params.cpf.endsWith("4") || params.cpf.endsWith("5")) {
            logger.debug("Mock: CPF termina em 4 ou 5, simulando usuário não encontrado")
            throw UserManagementException("Usuário não encontrado")
        }
        
        // Regra 2: Apenas partners "prevcom" e "caio" têm dados mockados
        return when (params.partner.lowercase()) {
            "prevcom" -> {
                logger.debug("Mock: Retornando dados do partner PREVCOM")
                createPrevcomUser(params)
            }
            "caio" -> {
                logger.debug("Mock: Retornando dados do partner CAIO")
                createCaioUser(params)
            }
            else -> {
                logger.debug("Mock: Partner '${params.partner}' não encontrado")
                throw UserManagementException("Partner não encontrado")
            }
        }
    }
    
    // MOCK: Dados específicos para o partner PREVCOM (Previdência)
    private fun createPrevcomUser(params: UserManagementGetUserParams): UserManagementGetUserResult {
        return UserManagementGetUserResult(
            userInfo = com.banco.fidc.auth.usecase.session.dto.result.UserInfoResult(
                cpf = params.cpf,
                fullName = "João Silva Santos",
                email = "joao.silva@email.com",
                birthDate = LocalDate.of(1985, 3, 15),
                phoneNumber = "+5511999887766"
            ),
            fund = com.banco.fidc.auth.usecase.session.dto.result.FundResult(
                id = "PREVCOM001",
                name = "Prevcom Previdência RS",
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
    
    // MOCK: Dados específicos para o partner CAIO (Investimentos)
    private fun createCaioUser(params: UserManagementGetUserParams): UserManagementGetUserResult {
        return UserManagementGetUserResult(
            userInfo = com.banco.fidc.auth.usecase.session.dto.result.UserInfoResult(
                cpf = params.cpf,
                fullName = "Maria Oliveira Costa",
                email = "maria.costa@empresa.com",
                birthDate = LocalDate.of(1990, 7, 22),
                phoneNumber = "+5511888776655"
            ),
            fund = com.banco.fidc.auth.usecase.session.dto.result.FundResult(
                id = "CAIO001",
                name = "CAIO Fundo de Investimento",
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
}