package com.banco.fidc.auth.usecase.session.dto.input

data class SelectRelationshipInput(
    val accessToken: String,
    val relationshipId: String,
    val partner: String,
    val clientIpAddress: String,
    val userAgent: String,
    val correlationId: String
)