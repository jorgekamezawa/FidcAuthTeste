package com.banco.fidc.auth.usecase.session.dto.output

import com.banco.fidc.auth.usecase.session.dto.common.UserInfoData
import com.banco.fidc.auth.usecase.session.dto.common.FundData
import com.banco.fidc.auth.usecase.session.dto.common.RelationshipData

data class CreateUserSessionOutput(
    val userInfo: UserInfoData,
    val fund: FundData,
    val relationshipList: List<RelationshipData>,
    val permissions: List<String>,
    val accessToken: String
)