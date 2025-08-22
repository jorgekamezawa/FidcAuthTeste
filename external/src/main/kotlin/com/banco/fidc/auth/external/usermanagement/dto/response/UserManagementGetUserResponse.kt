package com.banco.fidc.auth.external.usermanagement.dto.response

import com.banco.fidc.auth.usecase.session.dto.result.UserManagementGetUserResult
import com.banco.fidc.auth.usecase.session.dto.result.UserInfoResult
import com.banco.fidc.auth.usecase.session.dto.result.FundResult
import com.banco.fidc.auth.usecase.session.dto.result.RelationshipResult
import java.time.LocalDate

data class UserManagementGetUserResponse(
    val userInfo: UserInfoResponse,
    val fund: FundResponse,
    val relationshipList: List<RelationshipResponse>
)

data class UserInfoResponse(
    val cpf: String,
    val fullName: String,
    val email: String,
    val birthDate: LocalDate,
    val phoneNumber: String
)

data class FundResponse(
    val id: String,
    val name: String,
    val type: String
)

data class RelationshipResponse(
    val id: String,
    val type: String?,
    val name: String,
    val status: String,
    val contractNumber: String
)

// Mapper to result
fun UserManagementGetUserResponse.toResult(): UserManagementGetUserResult {
    return UserManagementGetUserResult(
        userInfo = this.userInfo.toUserInfoResult(),
        fund = this.fund.toFundResult(),
        relationshipList = this.relationshipList.map { it.toRelationshipResult() }
    )
}

fun UserInfoResponse.toUserInfoResult(): UserInfoResult {
    return UserInfoResult(
        cpf = this.cpf,
        fullName = this.fullName,
        email = this.email,
        birthDate = this.birthDate,
        phoneNumber = this.phoneNumber
    )
}

fun FundResponse.toFundResult(): FundResult {
    return FundResult(
        id = this.id,
        name = this.name,
        type = this.type
    )
}

fun RelationshipResponse.toRelationshipResult(): RelationshipResult {
    return RelationshipResult(
        id = this.id,
        type = this.type,
        name = this.name,
        status = this.status,
        contractNumber = this.contractNumber
    )
}