package com.banco.fidc.auth.domain.session.entity

import com.banco.fidc.auth.shared.exception.UserSessionControlValidationException
import com.banco.fidc.auth.shared.exception.UserSessionControlBusinessRuleException
import com.banco.fidc.auth.shared.constants.SessionConstants
import java.time.LocalDateTime
import java.util.UUID

class UserSessionControl private constructor(
    private var _id: Long,
    private var _externalId: UUID,
    private var _cpf: String,
    private var _partner: String,
    private var _currentSessionId: UUID?,
    private var _isActive: Boolean,
    private var _firstAccessAt: LocalDateTime?,
    private var _previousAccessAt: LocalDateTime?,
    private var _lastAccessAt: LocalDateTime,
    private var _createdAt: LocalDateTime,
    private var _updatedAt: LocalDateTime
) {
    val id: Long get() = _id
    val externalId: UUID get() = _externalId
    val cpf: String get() = _cpf
    val partner: String get() = _partner
    val currentSessionId: UUID? get() = _currentSessionId
    val isActive: Boolean get() = _isActive
    val firstAccessAt: LocalDateTime? get() = _firstAccessAt
    val previousAccessAt: LocalDateTime? get() = _previousAccessAt
    val lastAccessAt: LocalDateTime get() = _lastAccessAt
    val createdAt: LocalDateTime get() = _createdAt
    val updatedAt: LocalDateTime get() = _updatedAt
    
    companion object {
        fun createNew(
            cpf: String,
            partner: String
        ): UserSessionControl {
            validateCpf(cpf)
            validatePartner(partner)
            
            val now = LocalDateTime.now()
            
            return UserSessionControl(
                _id = 0L,
                _externalId = UUID.randomUUID(),
                _cpf = cpf.trim(),
                _partner = partner.trim(),
                _currentSessionId = null,
                _isActive = false,
                _firstAccessAt = null,
                _previousAccessAt = null,
                _lastAccessAt = now,
                _createdAt = now,
                _updatedAt = now
            )
        }
        
        fun reconstruct(
            id: Long,
            externalId: UUID,
            cpf: String,
            partner: String,
            currentSessionId: UUID?,
            isActive: Boolean,
            firstAccessAt: LocalDateTime?,
            previousAccessAt: LocalDateTime?,
            lastAccessAt: LocalDateTime,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime
        ): UserSessionControl {
            if (id <= 0) {
                throw UserSessionControlValidationException(
                    "ID deve ser maior que zero para reconstituir entidade"
                )
            }
            
            return UserSessionControl(
                _id = id,
                _externalId = externalId,
                _cpf = cpf,
                _partner = partner,
                _currentSessionId = currentSessionId,
                _isActive = isActive,
                _firstAccessAt = firstAccessAt,
                _previousAccessAt = previousAccessAt,
                _lastAccessAt = lastAccessAt,
                _createdAt = createdAt,
                _updatedAt = updatedAt
            )
        }
        
        private fun validateCpf(value: String) {
            if (value.isBlank()) {
                throw UserSessionControlValidationException("CPF não pode estar vazio")
            }
            
            if (value.length != SessionConstants.MAX_CPF_LENGTH) {
                throw UserSessionControlValidationException(
                    "CPF deve ter exatamente ${SessionConstants.MAX_CPF_LENGTH} dígitos"
                )
            }
            
            if (!value.all { it.isDigit() }) {
                throw UserSessionControlValidationException("CPF deve conter apenas dígitos")
            }
        }
        
        private fun validatePartner(value: String) {
            if (value.isBlank()) {
                throw UserSessionControlValidationException("Partner não pode estar vazio")
            }
            
            if (value.length > SessionConstants.MAX_PARTNER_LENGTH) {
                throw UserSessionControlValidationException(
                    "Partner não pode ter mais de ${SessionConstants.MAX_PARTNER_LENGTH} caracteres"
                )
            }
        }
    }
    
    fun startNewSession(sessionId: UUID) {
        val now = LocalDateTime.now()
        
        // Se é o primeiro acesso
        if (_firstAccessAt == null) {
            _firstAccessAt = now
        } else {
            // Se não é o primeiro acesso, salva o último como anterior
            _previousAccessAt = _lastAccessAt
        }
        
        _currentSessionId = sessionId
        _isActive = true
        _lastAccessAt = now
        _updatedAt = now
    }
    
    fun deactivateSession() {
        if (!_isActive) {
            throw UserSessionControlBusinessRuleException("Sessão já está inativa")
        }
        
        _isActive = false
        _updatedAt = LocalDateTime.now()
    }
    
    fun updateCurrentSessionId(sessionId: UUID) {
        _currentSessionId = sessionId
        _updatedAt = LocalDateTime.now()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserSessionControl) return false
        return _externalId == other._externalId
    }
    
    override fun hashCode(): Int = _externalId.hashCode()
    
    override fun toString(): String {
        return "UserSessionControl(id=$_id, externalId=$_externalId, " +
               "cpf=${_cpf.take(3)}***, partner=$_partner, " +
               "isActive=$_isActive, currentSessionId=$_currentSessionId)"
    }
}