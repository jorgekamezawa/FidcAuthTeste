package com.banco.fidc.auth.domain.session.entity

import com.banco.fidc.auth.domain.session.enum.SessionChannelEnum
import com.banco.fidc.auth.shared.exception.SessionValidationException
import com.banco.fidc.auth.shared.constants.SessionConstants
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Session private constructor(
    private var _sessionId: UUID,
    private var _createdAt: LocalDateTime,
    private var _updatedAt: LocalDateTime,
    private var _partner: String,
    private var _userAgent: String,
    private var _channel: SessionChannelEnum,
    private var _fingerprint: String,
    private var _sessionSecret: String,
    private var _userInfo: UserInfo,
    private var _fund: Fund,
    private var _relationshipList: List<Relationship>,
    private var _relationshipsSelected: Relationship?,
    private var _permissions: List<String>
) {
    val sessionId: UUID get() = _sessionId
    val createdAt: LocalDateTime get() = _createdAt
    val updatedAt: LocalDateTime get() = _updatedAt
    val partner: String get() = _partner
    val userAgent: String get() = _userAgent
    val channel: SessionChannelEnum get() = _channel
    val fingerprint: String get() = _fingerprint
    val sessionSecret: String get() = _sessionSecret
    val userInfo: UserInfo get() = _userInfo
    val fund: Fund get() = _fund
    val relationshipList: List<Relationship> get() = _relationshipList.toList()
    val relationshipsSelected: Relationship? get() = _relationshipsSelected
    val permissions: List<String> get() = _permissions.toList()
    
    companion object {
        fun create(
            partner: String,
            userAgent: String,
            channel: SessionChannelEnum,
            fingerprint: String,
            userInfo: UserInfo,
            fund: Fund,
            relationshipList: List<Relationship>,
            permissions: List<String>
        ): Session {
            validatePartner(partner)
            validateUserAgent(userAgent)
            validateFingerprint(fingerprint)
            
            val now = LocalDateTime.now()
            val sessionId = UUID.randomUUID()
            val sessionSecret = UUID.randomUUID().toString()
            
            return Session(
                _sessionId = sessionId,
                _createdAt = now,
                _updatedAt = now,
                _partner = partner.trim(),
                _userAgent = userAgent.trim(),
                _channel = channel,
                _fingerprint = fingerprint.trim(),
                _sessionSecret = sessionSecret,
                _userInfo = userInfo,
                _fund = fund,
                _relationshipList = relationshipList.toList(),
                _relationshipsSelected = null,
                _permissions = permissions.toList()
            )
        }
        
        fun reconstruct(
            sessionId: UUID,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
            partner: String,
            userAgent: String,
            channel: SessionChannelEnum,
            fingerprint: String,
            sessionSecret: String,
            userInfo: UserInfo,
            fund: Fund,
            relationshipList: List<Relationship>,
            relationshipsSelected: Relationship?,
            permissions: List<String>
        ): Session {
            return Session(
                _sessionId = sessionId,
                _createdAt = createdAt,
                _updatedAt = updatedAt,
                _partner = partner,
                _userAgent = userAgent,
                _channel = channel,
                _fingerprint = fingerprint,
                _sessionSecret = sessionSecret,
                _userInfo = userInfo,
                _fund = fund,
                _relationshipList = relationshipList.toList(),
                _relationshipsSelected = relationshipsSelected,
                _permissions = permissions.toList()
            )
        }
        
        private fun validatePartner(value: String) {
            if (value.isBlank()) {
                throw SessionValidationException("Partner não pode estar vazio")
            }
            
            if (value.length > SessionConstants.MAX_PARTNER_LENGTH) {
                throw SessionValidationException(
                    "Partner não pode ter mais de ${SessionConstants.MAX_PARTNER_LENGTH} caracteres"
                )
            }
        }
        
        private fun validateUserAgent(value: String) {
            if (value.isBlank()) {
                throw SessionValidationException("UserAgent não pode estar vazio")
            }
            
            if (value.length > SessionConstants.MAX_USER_AGENT_LENGTH) {
                throw SessionValidationException(
                    "UserAgent não pode ter mais de ${SessionConstants.MAX_USER_AGENT_LENGTH} caracteres"
                )
            }
        }
        
        private fun validateFingerprint(value: String) {
            if (value.isBlank()) {
                throw SessionValidationException("Fingerprint não pode estar vazio")
            }
            
            if (value.length > SessionConstants.MAX_FINGERPRINT_LENGTH) {
                throw SessionValidationException(
                    "Fingerprint não pode ter mais de ${SessionConstants.MAX_FINGERPRINT_LENGTH} caracteres"
                )
            }
        }
    }
    
    fun selectRelationship(relationship: Relationship) {
        if (!_relationshipList.any { it.id == relationship.id }) {
            throw SessionValidationException(
                "Relacionamento ${relationship.id} não existe na lista de relacionamentos da sessão"
            )
        }
        
        _relationshipsSelected = relationship
        _updatedAt = LocalDateTime.now()
    }
    
    fun updatePermissions(newPermissions: List<String>) {
        _permissions = newPermissions.toList()
        _updatedAt = LocalDateTime.now()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Session) return false
        return _sessionId == other._sessionId
    }
    
    override fun hashCode(): Int = _sessionId.hashCode()
    
    override fun toString(): String {
        return "Session(sessionId=$_sessionId, partner=$_partner, " +
               "channel=${_channel.description}, cpf=${_userInfo.cpf.take(3)}***)"
    }
}

data class UserInfo(
    val cpf: String,
    val fullName: String,
    val email: String,
    val birthDate: LocalDate,
    val phoneNumber: String
)

data class Fund(
    val id: String,
    val name: String,
    val type: String
)

data class Relationship(
    val id: String,
    val type: String?,
    val name: String,
    val status: String,
    val contractNumber: String
)