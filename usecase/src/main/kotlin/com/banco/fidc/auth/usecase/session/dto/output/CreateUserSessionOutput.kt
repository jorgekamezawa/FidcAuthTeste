package com.banco.fidc.auth.usecase.session.dto.output

import java.time.LocalDate
import java.util.*

data class CreateUserSessionOutput(
    val userInfo: UserInfoData,
    val fund: FundData,
    val relationshipList: List<RelationshipData>,
    val permissions: List<String>,
    val accessToken: String
)

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