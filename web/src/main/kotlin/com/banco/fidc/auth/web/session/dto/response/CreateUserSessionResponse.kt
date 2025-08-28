package com.banco.fidc.auth.web.session.dto.response

import com.banco.fidc.auth.usecase.session.dto.output.CreateUserSessionOutput
import com.banco.fidc.auth.web.session.dto.common.UserInfoResponse
import com.banco.fidc.auth.web.session.dto.common.FundResponse
import com.banco.fidc.auth.web.session.dto.common.RelationshipResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.format.DateTimeFormatter

@Schema(description = "Resposta após criar sessão de usuário")
data class CreateUserSessionResponse(
    @Schema(description = "Informações do usuário")
    val userInfo: UserInfoResponse,
    
    @Schema(description = "Informações do fundo")
    val fund: FundResponse,
    
    @Schema(description = "Relacionamentos do usuário")
    val relationshipList: List<RelationshipResponse>,
    
    @Schema(description = "Permissões do usuário")
    val permissions: List<String>,
    
    @Schema(description = "Token de acesso para requisições autenticadas")
    val accessToken: String
)

fun CreateUserSessionOutput.toResponse(): CreateUserSessionResponse {
    return CreateUserSessionResponse(
        userInfo = UserInfoResponse(
            cpf = maskCpf(this.userInfo.cpf),
            fullName = this.userInfo.fullName,
            email = maskEmail(this.userInfo.email),
            birthDate = this.userInfo.birthDate,
            phoneNumber = maskPhone(this.userInfo.phoneNumber)
        ),
        fund = FundResponse(
            id = this.fund.id,
            name = this.fund.name,
            type = this.fund.type
        ),
        relationshipList = this.relationshipList.map { rel ->
            RelationshipResponse(
                id = rel.id,
                type = rel.type,
                name = rel.name,
                status = rel.status,
                contractNumber = rel.contractNumber
            )
        },
        permissions = this.permissions,
        accessToken = this.accessToken
    )
}

private fun maskCpf(cpf: String): String {
    return if (cpf.length == 11) {
        "${cpf.substring(0, 3)}***${cpf.substring(6, 9)}-${cpf.substring(9)}"
    } else {
        "***"
    }
}

private fun maskEmail(email: String): String {
    val parts = email.split("@")
    if (parts.size != 2) return "***@***"
    
    val name = parts[0]
    val domain = parts[1]
    
    return if (name.length > 2) {
        "${name.first()}${"*".repeat(name.length - 2)}${name.last()}@$domain"
    } else {
        "${"*".repeat(name.length)}@$domain"
    }
}

private fun maskPhone(phone: String): String {
    val digits = phone.replace(Regex("[^0-9]"), "")
    return when (digits.length) {
        11 -> "(${digits.substring(0, 2)}) 9****-${digits.substring(7)}"
        10 -> "(${digits.substring(0, 2)}) ****-${digits.substring(6)}"
        else -> "****-****"
    }
}