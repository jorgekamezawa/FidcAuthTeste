package com.banco.fidc.auth.repository.redis.session.entity

import com.banco.fidc.auth.domain.session.entity.*
import com.banco.fidc.auth.domain.session.enum.SessionChannelEnum
import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class SessionRedisEntity(
    val sessionId: UUID,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val createdAt: LocalDateTime,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val updatedAt: LocalDateTime,
    
    val partner: String,
    val userAgent: String,
    val channel: String,
    val fingerprint: String,
    val sessionSecret: String,
    val userInfo: UserInfoRedis,
    val fund: FundRedis,
    val relationshipList: List<RelationshipRedis>,
    val relationshipsSelected: RelationshipRedis?,
    val permissions: List<String>,
    val ttlMinutes: Int
)

data class UserInfoRedis(
    val cpf: String,
    val fullName: String,
    val email: String,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    val birthDate: LocalDate,
    
    val phoneNumber: String
)

data class FundRedis(
    val id: String,
    val name: String,
    val type: String
)

data class RelationshipRedis(
    val id: String,
    val type: String?,
    val name: String,
    val status: String,
    val contractNumber: String
)

// Mappers como extension functions
fun Session.toRedisEntity(): SessionRedisEntity {
    return SessionRedisEntity(
        sessionId = this.sessionId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        partner = this.partner,
        userAgent = this.userAgent,
        channel = this.channel.name,
        fingerprint = this.fingerprint,
        sessionSecret = this.sessionSecret,
        userInfo = this.userInfo.toRedis(),
        fund = this.fund.toRedis(),
        relationshipList = this.relationshipList.map { it.toRedis() },
        relationshipsSelected = this.relationshipsSelected?.toRedis(),
        permissions = this.permissions,
        ttlMinutes = this.ttlMinutes
    )
}

fun SessionRedisEntity.toDomainEntity(): Session {
    val channel = SessionChannelEnum.fromValue(this.channel)
    
    return Session.reconstruct(
        sessionId = this.sessionId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        partner = this.partner,
        userAgent = this.userAgent,
        channel = channel,
        fingerprint = this.fingerprint,
        sessionSecret = this.sessionSecret,
        userInfo = this.userInfo.toDomain(),
        fund = this.fund.toDomain(),
        relationshipList = this.relationshipList.map { it.toDomain() },
        relationshipsSelected = this.relationshipsSelected?.toDomain(),
        permissions = this.permissions,
        ttlMinutes = this.ttlMinutes
    )
}

// Extension functions para conversão dos value objects
private fun UserInfo.toRedis(): UserInfoRedis {
    return UserInfoRedis(
        cpf = this.cpf,
        fullName = this.fullName,
        email = this.email,
        birthDate = this.birthDate,
        phoneNumber = this.phoneNumber
    )
}

private fun UserInfoRedis.toDomain(): UserInfo {
    return UserInfo(
        cpf = this.cpf,
        fullName = this.fullName,
        email = this.email,
        birthDate = this.birthDate,
        phoneNumber = this.phoneNumber
    )
}

private fun Fund.toRedis(): FundRedis {
    return FundRedis(
        id = this.id,
        name = this.name,
        type = this.type
    )
}

private fun FundRedis.toDomain(): Fund {
    return Fund(
        id = this.id,
        name = this.name,
        type = this.type
    )
}

private fun Relationship.toRedis(): RelationshipRedis {
    return RelationshipRedis(
        id = this.id,
        type = this.type,
        name = this.name,
        status = this.status,
        contractNumber = this.contractNumber
    )
}

private fun RelationshipRedis.toDomain(): Relationship {
    return Relationship(
        id = this.id,
        type = this.type,
        name = this.name,
        status = this.status,
        contractNumber = this.contractNumber
    )
}