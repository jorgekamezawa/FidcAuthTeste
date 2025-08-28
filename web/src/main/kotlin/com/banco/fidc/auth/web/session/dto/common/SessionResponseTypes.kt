package com.banco.fidc.auth.web.session.dto.common

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Informações do usuário")
data class UserInfoResponse(
    @Schema(description = "CPF mascarado", example = "123***456-78")
    val cpf: String,
    
    @Schema(description = "Nome completo", example = "João Silva Santos")
    val fullName: String,
    
    @Schema(description = "Email mascarado", example = "j***@email.com")
    val email: String,
    
    @Schema(description = "Data de nascimento", example = "1985-05-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val birthDate: LocalDate,
    
    @Schema(description = "Telefone mascarado", example = "(11) 9****-1234")
    val phoneNumber: String
)

@Schema(description = "Informações do fundo")
data class FundResponse(
    @Schema(description = "ID do fundo", example = "fund-123")
    val id: String,
    
    @Schema(description = "Nome do fundo", example = "FIDC ABC")
    val name: String,
    
    @Schema(description = "Tipo do fundo", example = "FIDC")
    val type: String
)

@Schema(description = "Informações de relacionamento")
data class RelationshipResponse(
    @Schema(description = "ID do relacionamento", example = "rel-456")
    val id: String,
    
    @Schema(description = "Tipo de relacionamento", example = "CEDENTE")
    val type: String?,
    
    @Schema(description = "Nome do relacionamento", example = "Empresa XYZ Ltda")
    val name: String,
    
    @Schema(description = "Status", example = "ACTIVE")
    val status: String,
    
    @Schema(description = "Número do contrato", example = "CTR-001")
    val contractNumber: String
)