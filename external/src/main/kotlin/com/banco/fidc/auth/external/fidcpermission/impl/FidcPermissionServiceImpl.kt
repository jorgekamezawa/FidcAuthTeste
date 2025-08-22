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

    // MOCK: 3 CENÁRIOS DIFERENTES - REMOVER QUANDO A API ESTIVER PRONTA  
    private fun getMockPermissions(params: FidcPermissionGetPermissionsParams): FidcPermissionGetPermissionsResult {
        logger.warn("USANDO DADOS MOCKADOS - FidcPermission API não disponível ainda")
        
        return when (params.partner.lowercase()) {
            "prevcom" -> createPrevcomPermissions(params)
            "caio" -> createCaioPermissions(params)
            else -> createDefaultPermissions(params)
        }
    }
    
    private fun createPrevcomPermissions(params: FidcPermissionGetPermissionsParams): FidcPermissionGetPermissionsResult {
        val permissions = if (params.relationshipId == null) {
            // Permissões gerais para PREVCOM
            listOf(
                "VIEW_PROFILE",
                "VIEW_STATEMENTS", 
                "VIEW_PLAN_DETAILS",
                "VIEW_CONTRIBUTIONS",
                "DOWNLOAD_DOCUMENTS",
                "UPDATE_PERSONAL_DATA",
                "REQUEST_PORTABILITY"
            )
        } else {
            // Permissões específicas por relacionamento
            listOf(
                "VIEW_PROFILE",
                "VIEW_STATEMENTS",
                "VIEW_PLAN_DETAILS", 
                "VIEW_CONTRIBUTIONS",
                "DOWNLOAD_DOCUMENTS",
                "UPDATE_PERSONAL_DATA",
                "REQUEST_PORTABILITY",
                "MANAGE_PLAN_CONTRIBUTIONS",
                "REQUEST_PLAN_CHANGES"
            )
        }
        
        return FidcPermissionGetPermissionsResult(permissions)
    }
    
    private fun createCaioPermissions(params: FidcPermissionGetPermissionsParams): FidcPermissionGetPermissionsResult {
        val permissions = if (params.relationshipId == null) {
            // Permissões gerais para CAIO
            listOf(
                "VIEW_PROFILE",
                "VIEW_CONTRACTS",
                "CREATE_SIMULATION",
                "VIEW_INVESTMENT_PORTFOLIO",
                "DOWNLOAD_REPORTS"
            )
        } else {
            // Permissões específicas por relacionamento
            listOf(
                "VIEW_PROFILE",
                "VIEW_CONTRACTS",
                "CREATE_SIMULATION",
                "VIEW_INVESTMENT_PORTFOLIO",
                "DOWNLOAD_REPORTS",
                "EXECUTE_OPERATIONS",
                "MANAGE_INVESTMENTS",
                "TRANSFER_FUNDS"
            )
        }
        
        return FidcPermissionGetPermissionsResult(permissions)
    }
    
    private fun createDefaultPermissions(params: FidcPermissionGetPermissionsParams): FidcPermissionGetPermissionsResult {
        val permissions = if (params.relationshipId == null) {
            // Permissões básicas para outros partners
            listOf(
                "VIEW_PROFILE",
                "VIEW_BASIC_DATA",
                "DOWNLOAD_DOCUMENTS"
            )
        } else {
            // Permissões específicas por relacionamento
            listOf(
                "VIEW_PROFILE",
                "VIEW_BASIC_DATA", 
                "DOWNLOAD_DOCUMENTS",
                "VIEW_RELATIONSHIP_DETAILS"
            )
        }
        
        return FidcPermissionGetPermissionsResult(permissions)
    }
}