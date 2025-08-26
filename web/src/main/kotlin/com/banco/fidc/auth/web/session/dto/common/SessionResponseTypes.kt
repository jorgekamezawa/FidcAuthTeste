package com.banco.fidc.auth.web.session.dto.common

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "User information")
data class UserInfoResponse(
    @Schema(description = "Masked CPF", example = "123***456-78")
    val cpf: String,
    
    @Schema(description = "Full name", example = "João Silva Santos")
    val fullName: String,
    
    @Schema(description = "Masked email", example = "j***@email.com")
    val email: String,
    
    @Schema(description = "Birth date", example = "1985-05-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val birthDate: LocalDate,
    
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