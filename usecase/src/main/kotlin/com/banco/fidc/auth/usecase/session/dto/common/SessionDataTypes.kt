package com.banco.fidc.auth.usecase.session.dto.common

import com.banco.fidc.auth.domain.session.entity.*
import java.time.LocalDate

data class UserInfoData(
    val cpf: String,
    val fullName: String,
    val email: String,
    val birthDate: LocalDate,
    val phoneNumber: String
)

data class FundData(
    val id: String,
    val name: String,
    val type: String
)

data class RelationshipData(
    val id: String,
    val type: String?,
    val name: String,
    val status: String,
    val contractNumber: String
)

// Extension functions para mapear entre domain e data classes
fun UserInfo.toUserInfoData(): UserInfoData {
    return UserInfoData(
        cpf = this.cpf,
        fullName = this.fullName,
        email = this.email,
        birthDate = this.birthDate,
        phoneNumber = this.phoneNumber
    )
}

fun Fund.toFundData(): FundData {
    return FundData(
        id = this.id,
        name = this.name,
        type = this.type
    )
}

fun Relationship.toRelationshipData(): RelationshipData {
    return RelationshipData(
        id = this.id,
        type = this.type,
        name = this.name,
        status = this.status,
        contractNumber = this.contractNumber
    )
}