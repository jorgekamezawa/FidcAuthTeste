package com.banco.fidc.auth.external.fidcpermission.impl

import com.banco.fidc.auth.external.fidcpermission.client.FidcPermissionFeignClient
import com.banco.fidc.auth.external.fidcpermission.dto.response.toResult
import com.banco.fidc.auth.external.fidcpermission.exception.FidcPermissionException
import com.banco.fidc.auth.external.config.feign.FeignCallHelper
import com.banco.fidc.auth.usecase.session.service.FidcPermissionService
import com.banco.fidc.auth.usecase.session.dto.params.FidcPermissionGetPermissionsParams
import com.banco.fidc.auth.usecase.session.dto.result.FidcPermissionGetPermissionsResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FidcPermissionServiceImpl(
    private val feignClient: FidcPermissionFeignClient,
    private val feignCallHelper: FeignCallHelper
) : FidcPermissionService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getPermissions(params: FidcPermissionGetPermissionsParams): FidcPermissionGetPermissionsResult {
        logger.debug("Getting permissions from FidcPermission: partner=${params.partner}, cpf=${params.cpf.take(3)}***, relationshipId=${params.relationshipId}")

        // TODO: QUANDO A API ESTIVER PRONTA, DESCOMENTE A LINHA ABAIXO E COMENTE O MÉTODO MOCKADO
        // return callRealApi(params)
        
        // MOCK: REMOVER QUANDO A API ESTIVER PRONTA
        return getMockPermissions(params)
    }
    
    // MÉTODO REAL PARA QUANDO A API ESTIVER PRONTA
    private fun callRealApi(params: FidcPermissionGetPermissionsParams): FidcPermissionGetPermissionsResult {
        val response = feignCallHelper.executeFeignCall(
            componentName = "FidcPermission",
            operation = "getPermissions",
            call = { feignClient.getPermissions(params.partner, params.cpf, params.relationshipId) },
            createException = { msg, cause -> FidcPermissionException(msg, cause) },
            handle404AsNull = true  // Sem permissões retorna lista vazia
        )

        return response?.toResult() ?: FidcPermissionGetPermissionsResult(emptyList())
    }

    // MOCK: BASEADO EM PARTNER, CPF E RELATIONSHIP - REMOVER QUANDO A API ESTIVER PRONTA
    // 
    // REGRAS DO MOCK (ALINHADO COM UserManagementService):
    // 1. Partners suportados: "prevcom" e "caio" (case-insensitive)
    // 2. CPF terminando em 4 ou 5: simula usuário sem permissões
    // 3. Qualquer outro partner: simula partner sem permissões
    // 4. relationshipId = null: permissões gerais (menos permissões)
    // 5. relationshipId preenchido: permissões específicas (mais permissões)
    //
    // RELATIONSHIPS VÁLIDOS:
    // PREVCOM: REL001 (Plano Básico), REL002 (Plano Premium)
    // CAIO: REL003 (Conta Investimentos Master)
    //
    private fun getMockPermissions(params: FidcPermissionGetPermissionsParams): FidcPermissionGetPermissionsResult {
        logger.warn("USANDO DADOS MOCKADOS - FidcPermission API não disponível ainda")
        
        // Regra 1: CPF terminando em 4 ou 5 = sem permissões (simula casos de erro)
        if (params.cpf.endsWith("4") || params.cpf.endsWith("5")) {
            logger.debug("Mock: CPF termina em 4 ou 5, retornando sem permissões")
            return FidcPermissionGetPermissionsResult(emptyList())
        }
        
        // Regra 2: Apenas partners "prevcom" e "caio" têm permissões
        return when (params.partner.lowercase()) {
            "prevcom" -> {
                logger.debug("Mock: Retornando permissões do partner PREVCOM, relationshipId=${params.relationshipId}")
                createPrevcomPermissions(params)
            }
            "caio" -> {
                logger.debug("Mock: Retornando permissões do partner CAIO, relationshipId=${params.relationshipId}")
                createCaioPermissions(params)
            }
            else -> {
                logger.debug("Mock: Partner '${params.partner}' sem permissões")
                FidcPermissionGetPermissionsResult(emptyList())
            }
        }
    }
    
    // MOCK: Dados específicos para o partner PREVCOM (Previdência)
    private fun createPrevcomPermissions(params: FidcPermissionGetPermissionsParams): FidcPermissionGetPermissionsResult {
        val permissions = if (params.relationshipId == null) {
            // Permissões gerais PREVCOM (sem relationship selecionado)
            listOf(
                "VIEW_PROFILE",
                "VIEW_PLAN_SUMMARY",
                "DOWNLOAD_BASIC_DOCUMENTS"
            )
        } else {
            // Permissões específicas por relacionamento PREVCOM
            when (params.relationshipId) {
                "REL001" -> {
                    // REL001 - Plano Previdência Básico (menos permissões)
                    listOf(
                        "VIEW_PROFILE",
                        "VIEW_PLAN_SUMMARY",
                        "DOWNLOAD_BASIC_DOCUMENTS",
                        "VIEW_PLAN_DETAILS",
                        "CREATE_SIMULATION",
                        "VIEW_CONTRIBUTIONS_HISTORY",
                        "DOWNLOAD_PLAN_STATEMENT"
                    )
                }
                "REL002" -> {
                    // REL002 - Plano Previdência Premium (mais permissões)
                    listOf(
                        "VIEW_PROFILE",
                        "VIEW_PLAN_SUMMARY", 
                        "DOWNLOAD_BASIC_DOCUMENTS",
                        "VIEW_PLAN_DETAILS",
                        "VIEW_CONTRIBUTIONS_HISTORY",
                        "DOWNLOAD_PLAN_STATEMENT",
                        "UPDATE_BENEFICIARIES",
                        "REQUEST_LOAN",
                        "MANAGE_PLAN_CONTRIBUTIONS",
                        "REQUEST_PLAN_CHANGES",
                        "VIEW_INVESTMENT_OPTIONS"
                    )
                }
                else -> {
                    // Relationship não encontrado - permissões básicas
                    logger.debug("Mock: RelationshipId '${params.relationshipId}' não encontrado para PREVCOM")
                    listOf(
                        "VIEW_PROFILE",
                        "VIEW_PLAN_SUMMARY"
                    )
                }
            }
        }
        
        return FidcPermissionGetPermissionsResult(permissions)
    }
    
    // MOCK: Dados específicos para o partner CAIO (Investimentos)
    private fun createCaioPermissions(params: FidcPermissionGetPermissionsParams): FidcPermissionGetPermissionsResult {
        val permissions = if (params.relationshipId == null) {
            // Permissões gerais CAIO (sem relationship selecionado)
            listOf(
                "VIEW_PROFILE",
                "VIEW_PORTFOLIO_SUMMARY",
                "DOWNLOAD_BASIC_REPORTS"
            )
        } else {
            // Permissões específicas por relacionamento CAIO
            when (params.relationshipId) {
                "REL003" -> {
                    // REL003 - Conta Investimentos Master (permissões completas)
                    listOf(
                        "VIEW_PROFILE",
                        "VIEW_PORTFOLIO_SUMMARY",
                        "DOWNLOAD_BASIC_REPORTS",
                        "VIEW_INVESTMENT_DETAILS",
                        "VIEW_TRANSACTION_HISTORY",
                        "DOWNLOAD_DETAILED_REPORTS",
                        "CREATE_INVESTMENT_SIMULATION",
                        "EXECUTE_BUY_ORDERS",
                        "EXECUTE_SELL_ORDERS",
                        "TRANSFER_BETWEEN_FUNDS",
                        "MANAGE_AUTOMATIC_INVESTMENTS",
                        "REQUEST_INVESTMENT_ADVICE"
                    )
                }
                else -> {
                    // Relationship não encontrado - permissões básicas
                    logger.debug("Mock: RelationshipId '${params.relationshipId}' não encontrado para CAIO")
                    listOf(
                        "VIEW_PROFILE",
                        "VIEW_PORTFOLIO_SUMMARY"
                    )
                }
            }
        }
        
        return FidcPermissionGetPermissionsResult(permissions)
    }
}