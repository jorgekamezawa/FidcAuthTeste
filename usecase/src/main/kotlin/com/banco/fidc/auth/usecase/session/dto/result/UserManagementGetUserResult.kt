package com.banco.fidc.auth.usecase.session.dto.result

import com.banco.fidc.auth.usecase.session.dto.common.UserInfoData
import com.banco.fidc.auth.usecase.session.dto.common.FundData
import com.banco.fidc.auth.usecase.session.dto.common.RelationshipData
import java.time.LocalDate

data class UserManagementGetUserResult(
    val userInfo: UserInfoResult,
    val fund: FundResult,
    val relationshipList: List<RelationshipResult>
)

data class UserInfoResult(
    val cpf: String,
    val fullName: String,
    val email: String,
    val birthDate: LocalDate,
    val phoneNumber: String
)

data class FundResult(
    val id: String,
    val name: String,
    val type: String
)

data class RelationshipResult(
    val id: String,
    val type: String?,
    val name: String,
    val status: String,
    val contractNumber: String
)

fun UserManagementGetUserResult.toOutputData(): Triple<UserInfoData, FundData, List<RelationshipData>> {
    return Triple(
        this.userInfo.toUserInfoData(),
        this.fund.toFundData(),
        this.relationshipList.map { it.toRelationshipData() }
    )
}

fun UserInfoResult.toUserInfoData(): UserInfoData {
    return UserInfoData(
        cpf = this.cpf,
        fullName = this.fullName,
        email = this.email,
        birthDate = this.birthDate,
        phoneNumber = this.phoneNumber
    )
}

fun FundResult.toFundData(): FundData {
    return FundData(
        id = this.id,
        name = this.name,
        type = this.type
    )
}

fun RelationshipResult.toRelationshipData(): RelationshipData {
    return RelationshipData(
        id = this.id,
        type = this.type,
        name = this.name,
        status = this.status,
        contractNumber = this.contractNumber
    )
}