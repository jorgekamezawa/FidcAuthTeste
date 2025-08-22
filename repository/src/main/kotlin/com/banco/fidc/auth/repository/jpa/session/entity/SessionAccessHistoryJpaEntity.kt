package com.banco.fidc.auth.repository.jpa.session.entity

import com.banco.fidc.auth.domain.session.entity.SessionAccessHistory
import jakarta.persistence.*
import java.math.BigDecimal
import java.net.InetAddress
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "session_access_history",
    indexes = [
        Index(name = "idx_session_access_history_uuid", columnList = "uuid"),
        Index(name = "idx_session_access_history_user_session_control_id", columnList = "user_session_control_id"),
        Index(name = "idx_session_access_history_session_id", columnList = "session_id"),
        Index(name = "idx_session_access_history_occurred_at", columnList = "occurred_at"),
        Index(name = "idx_session_access_history_ip_address", columnList = "ip_address")
    ]
)
class SessionAccessHistoryJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long,
    
    @Column(name = "uuid", nullable = false, unique = true, columnDefinition = "UUID")
    var uuid: UUID,
    
    @Column(name = "user_session_control_id", nullable = false)
    var userSessionControlId: Long,
    
    @Column(name = "session_id", nullable = false, columnDefinition = "UUID")
    var sessionId: UUID,
    
    @Column(name = "occurred_at", nullable = false, columnDefinition = "TIMESTAMP")
    var occurredAt: LocalDateTime,
    
    @Column(name = "ip_address", columnDefinition = "INET")
    var ipAddress: InetAddress?,
    
    @Column(name = "user_agent", nullable = false, columnDefinition = "TEXT")
    var userAgent: String,
    
    @Column(name = "latitude", precision = 10, scale = 8)
    var latitude: BigDecimal?,
    
    @Column(name = "longitude", precision = 11, scale = 8)
    var longitude: BigDecimal?,
    
    @Column(name = "location_accuracy")
    var locationAccuracy: Int?,
    
    @Column(name = "location_timestamp", columnDefinition = "TIMESTAMP")
    var locationTimestamp: LocalDateTime?,
    
    // Relacionamento com UserSessionControl
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_session_control_id", insertable = false, updatable = false)
    var userSessionControl: UserSessionControlJpaEntity? = null
)

// Mappers como extension functions
fun SessionAccessHistory.toJpaEntity(): SessionAccessHistoryJpaEntity {
    return SessionAccessHistoryJpaEntity(
        id = this.id,
        uuid = this.externalId,
        userSessionControlId = this.userSessionControlId,
        sessionId = this.sessionId,
        occurredAt = this.occurredAt,
        ipAddress = this.ipAddress,
        userAgent = this.userAgent,
        latitude = this.latitude,
        longitude = this.longitude,
        locationAccuracy = this.locationAccuracy,
        locationTimestamp = this.locationTimestamp,
        userSessionControl = null // Relacionamento será gerenciado pelo JPA
    )
}

fun SessionAccessHistoryJpaEntity.toDomainEntity(): SessionAccessHistory {
    return SessionAccessHistory.reconstruct(
        id = this.id,
        externalId = this.uuid,
        userSessionControlId = this.userSessionControlId,
        sessionId = this.sessionId,
        occurredAt = this.occurredAt,
        ipAddress = this.ipAddress,
        userAgent = this.userAgent,
        latitude = this.latitude,
        longitude = this.longitude,
        locationAccuracy = this.locationAccuracy,
        locationTimestamp = this.locationTimestamp
    )
}