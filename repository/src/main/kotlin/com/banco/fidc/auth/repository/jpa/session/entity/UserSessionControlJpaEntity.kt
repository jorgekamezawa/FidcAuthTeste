package com.banco.fidc.auth.repository.jpa.session.entity

import com.banco.fidc.auth.domain.session.entity.UserSessionControl
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "tb_user_session_control",
    indexes = [
        Index(name = "idx_user_session_control_uuid", columnList = "uuid"),
        Index(name = "idx_user_session_control_cpf_partner", columnList = "cpf, partner"),
        Index(name = "idx_user_session_control_current_session_id", columnList = "current_session_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_session_control_cpf_partner", columnNames = ["cpf", "partner"])
    ]
)
class UserSessionControlJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long,
    
    @Column(name = "uuid", nullable = false, unique = true, columnDefinition = "UUID")
    var uuid: UUID,
    
    @Column(name = "cpf", nullable = false, length = 11, columnDefinition = "VARCHAR(11)")
    var cpf: String,
    
    @Column(name = "partner", nullable = false, length = 100, columnDefinition = "VARCHAR(100)")
    var partner: String,
    
    @Column(name = "current_session_id", columnDefinition = "UUID")
    var currentSessionId: UUID?,
    
    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    var isActive: Boolean,
    
    @Column(name = "first_access_at", columnDefinition = "TIMESTAMP")
    var firstAccessAt: LocalDateTime?,
    
    @Column(name = "previous_access_at", columnDefinition = "TIMESTAMP")
    var previousAccessAt: LocalDateTime?,
    
    @Column(name = "last_access_at", nullable = false, columnDefinition = "TIMESTAMP")
    var lastAccessAt: LocalDateTime,
    
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP")
    var createdAt: LocalDateTime,
    
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP")
    var updatedAt: LocalDateTime
)

// Mappers como extension functions
fun UserSessionControl.toJpaEntity(): UserSessionControlJpaEntity {
    return UserSessionControlJpaEntity(
        id = this.id,
        uuid = this.externalId,
        cpf = this.cpf,
        partner = this.partner,
        currentSessionId = this.currentSessionId,
        isActive = this.isActive,
        firstAccessAt = this.firstAccessAt,
        previousAccessAt = this.previousAccessAt,
        lastAccessAt = this.lastAccessAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun UserSessionControlJpaEntity.toDomainEntity(): UserSessionControl {
    return UserSessionControl.reconstruct(
        id = this.id,
        externalId = this.uuid,
        cpf = this.cpf,
        partner = this.partner,
        currentSessionId = this.currentSessionId,
        isActive = this.isActive,
        firstAccessAt = this.firstAccessAt,
        previousAccessAt = this.previousAccessAt,
        lastAccessAt = this.lastAccessAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}