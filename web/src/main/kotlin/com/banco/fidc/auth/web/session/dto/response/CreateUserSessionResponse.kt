package com.banco.fidc.auth.web.session.dto.response

import com.banco.fidc.auth.usecase.session.dto.output.CreateUserSessionOutput
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Schema(description = "Response after creating user session")
data class CreateUserSessionResponse(
    @Schema(description = "User information")
    val userInfo: UserInfoResponse,
    
    @Schema(description = "Fund information")
    val fund: FundResponse,
    
    @Schema(description = "User relationships")
    val relationshipList: List<RelationshipResponse>,
    
    @Schema(description = "User permissions")
    val permissions: List<String>,
    
    @Schema(description = "Access token for authenticated requests")
    val accessToken: String
)

@Schema(description = "User information")
data class UserInfoResponse(
    @Schema(description = "Masked CPF", example = "123***456-78")
    val cpf: String,
    
    @Schema(description = "Full name", example = "João Silva Santos")
    val fullName: String,
    
    @Schema(description = "Masked email", example = "j***@email.com")
    val email: String,
    
    @Schema(description = "Birth date", example = "1985-05-15")
    val birthDate: String,
    
    @Schema(description = "Masked phone", example = "(11) 9****-1234")
    val phoneNumber: String
)

@Schema(description = "Fund information")
data class FundResponse(
    @Schema(description = "Fund ID", example = "fund-123")
    val id: String,
    
    @Schema(description = "Fund name", example = "FIDC ABC")
    val name: String,
    
    @Schema(description = "Fund type", example = "FIDC")
    val type: String
)

@Schema(description = "Relationship information")
data class RelationshipResponse(
    @Schema(description = "Relationship ID", example = "rel-456")
    val id: String,
    
    @Schema(description = "Relationship type", example = "CEDENTE")
    val type: String?,
    
    @Schema(description = "Relationship name", example = "Empresa XYZ Ltda")
    val name: String,
    
    @Schema(description = "Status", example = "ACTIVE")
    val status: String,
    
    @Schema(description = "Contract number", example = "CTR-001")
    val contractNumber: String
)

fun CreateUserSessionOutput.toResponse(): CreateUserSessionResponse {
    return CreateUserSessionResponse(
        userInfo = UserInfoResponse(
            cpf = maskCpf(this.userInfo.cpf),
            fullName = this.userInfo.fullName,
            email = maskEmail(this.userInfo.email),
            birthDate = this.userInfo.birthDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
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