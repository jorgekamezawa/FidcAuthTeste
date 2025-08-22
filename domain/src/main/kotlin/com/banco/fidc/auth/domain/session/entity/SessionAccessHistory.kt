package com.banco.fidc.auth.domain.session.entity

import com.banco.fidc.auth.shared.exception.SessionValidationException
import com.banco.fidc.auth.shared.constants.SessionConstants
import java.math.BigDecimal
import java.net.InetAddress
import java.time.LocalDateTime
import java.util.UUID

class SessionAccessHistory private constructor(
    private var _id: Long,
    private var _externalId: UUID,
    private var _userSessionControlId: Long,
    private var _sessionId: UUID,
    private var _occurredAt: LocalDateTime,
    private var _ipAddress: InetAddress?,
    private var _userAgent: String,
    private var _latitude: BigDecimal?,
    private var _longitude: BigDecimal?,
    private var _locationAccuracy: Int?,
    private var _locationTimestamp: LocalDateTime?
) {
    val id: Long get() = _id
    val externalId: UUID get() = _externalId
    val userSessionControlId: Long get() = _userSessionControlId
    val sessionId: UUID get() = _sessionId
    val occurredAt: LocalDateTime get() = _occurredAt
    val ipAddress: InetAddress? get() = _ipAddress
    val userAgent: String get() = _userAgent
    val latitude: BigDecimal? get() = _latitude
    val longitude: BigDecimal? get() = _longitude
    val locationAccuracy: Int? get() = _locationAccuracy
    val locationTimestamp: LocalDateTime? get() = _locationTimestamp
    
    companion object {
        fun createNew(
            userSessionControlId: Long,
            sessionId: UUID,
            ipAddress: InetAddress?,
            userAgent: String,
            latitude: BigDecimal?,
            longitude: BigDecimal?,
            locationAccuracy: Int?,
            locationTimestamp: LocalDateTime?
        ): SessionAccessHistory {
            validateUserSessionControlId(userSessionControlId)
            validateUserAgent(userAgent)
            validateLocationData(latitude, longitude, locationAccuracy)
            
            val now = LocalDateTime.now()
            
            return SessionAccessHistory(
                _id = 0L,
                _externalId = UUID.randomUUID(),
                _userSessionControlId = userSessionControlId,
                _sessionId = sessionId,
                _occurredAt = now,
                _ipAddress = ipAddress,
                _userAgent = userAgent.trim(),
                _latitude = latitude,
                _longitude = longitude,
                _locationAccuracy = locationAccuracy,
                _locationTimestamp = locationTimestamp
            )
        }
        
        fun reconstruct(
            id: Long,
            externalId: UUID,
            userSessionControlId: Long,
            sessionId: UUID,
            occurredAt: LocalDateTime,
            ipAddress: InetAddress?,
            userAgent: String,
            latitude: BigDecimal?,
            longitude: BigDecimal?,
            locationAccuracy: Int?,
            locationTimestamp: LocalDateTime?
        ): SessionAccessHistory {
            if (id <= 0) {
                throw SessionValidationException(
                    "ID deve ser maior que zero para reconstituir entidade"
                )
            }
            
            return SessionAccessHistory(
                _id = id,
                _externalId = externalId,
                _userSessionControlId = userSessionControlId,
                _sessionId = sessionId,
                _occurredAt = occurredAt,
                _ipAddress = ipAddress,
                _userAgent = userAgent,
                _latitude = latitude,
                _longitude = longitude,
                _locationAccuracy = locationAccuracy,
                _locationTimestamp = locationTimestamp
            )
        }
        
        private fun validateUserSessionControlId(value: Long) {
            if (value <= 0) {
                throw SessionValidationException("UserSessionControlId deve ser maior que zero")
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
        
        private fun validateLocationData(
            latitude: BigDecimal?,
            longitude: BigDecimal?,
            locationAccuracy: Int?
        ) {
            // Se latitude ou longitude forem fornecidas, ambas devem estar presentes
            if ((latitude != null && longitude == null) || (latitude == null && longitude != null)) {
                throw SessionValidationException(
                    "Latitude e longitude devem ser fornecidas em conjunto"
                )
            }
            
            // Validar range de latitude
            latitude?.let {
                if (it < BigDecimal("-90") || it > BigDecimal("90")) {
                    throw SessionValidationException("Latitude deve estar entre -90 e 90 graus")
                }
            }
            
            // Validar range de longitude
            longitude?.let {
                if (it < BigDecimal("-180") || it > BigDecimal("180")) {
                    throw SessionValidationException("Longitude deve estar entre -180 e 180 graus")
                }
            }
            
            // Validar precisão da localização
            locationAccuracy?.let {
                if (it < 0) {
                    throw SessionValidationException("Precisão da localização não pode ser negativa")
                }
            }
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionAccessHistory) return false
        return _externalId == other._externalId
    }
    
    override fun hashCode(): Int = _externalId.hashCode()
    
    override fun toString(): String {
        return "SessionAccessHistory(id=$_id, externalId=$_externalId, " +
               "sessionId=$_sessionId, occurredAt=$_occurredAt, " +
               "ipAddress=$_ipAddress, hasLocation=${_latitude != null})"
    }
}