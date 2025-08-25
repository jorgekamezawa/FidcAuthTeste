package com.banco.fidc.auth.usecase.session.dto.output

import com.banco.fidc.auth.domain.session.entity.*
import com.banco.fidc.auth.usecase.session.dto.common.*

data class SelectRelationshipOutput(
    val userInfo: UserInfoData,
    val fund: FundData,
    val relationshipList: List<RelationshipData>,
    val relationshipSelected: RelationshipData,
    val permissions: List<String>,
    val accessToken: String
)

// Extension function para mapear Session completa para SelectRelationshipOutput
fun Session.toSelectRelationshipOutput(accessToken: String): SelectRelationshipOutput {
    return SelectRelationshipOutput(
        userInfo = this.userInfo.toUserInfoData(),
        fund = this.fund.toFundData(),
        relationshipList = this.relationshipList.map { it.toRelationshipData() },
        relationshipSelected = this.relationshipsSelected!!.toRelationshipData(),
        permissions = this.permissions,
        accessToken = accessToken
    )
}