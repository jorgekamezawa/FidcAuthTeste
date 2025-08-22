package com.banco.fidc.auth.repository.jpa.session.repository

import com.banco.fidc.auth.repository.jpa.session.entity.SessionAccessHistoryJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.net.InetAddress
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface SessionAccessHistoryJpaRepository : JpaRepository<SessionAccessHistoryJpaEntity, Long> {
    
    fun findByUuid(uuid: UUID): SessionAccessHistoryJpaEntity?
    
    fun findBySessionId(sessionId: UUID): List<SessionAccessHistoryJpaEntity>
    
    fun findByUserSessionControlId(userSessionControlId: Long): List<SessionAccessHistoryJpaEntity>
    
    fun findByUserSessionControlIdOrderByOccurredAtDesc(userSessionControlId: Long): List<SessionAccessHistoryJpaEntity>
    
    fun findTopByUserSessionControlIdOrderByOccurredAtDesc(userSessionControlId: Long): SessionAccessHistoryJpaEntity?
    
    fun findByIpAddress(ipAddress: InetAddress): List<SessionAccessHistoryJpaEntity>
    
    @Query("""
        SELECT s FROM SessionAccessHistoryJpaEntity s 
        WHERE DATE(s.occurredAt) BETWEEN :startDate AND :endDate
        ORDER BY s.occurredAt DESC
    """)
    fun findByDateRange(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<SessionAccessHistoryJpaEntity>
    
    @Query("""
        SELECT s FROM SessionAccessHistoryJpaEntity s 
        WHERE s.userSessionControlId = :userSessionControlId
        AND DATE(s.occurredAt) BETWEEN :startDate AND :endDate
        ORDER BY s.occurredAt DESC
    """)
    fun findByUserSessionControlIdAndDateRange(
        @Param("userSessionControlId") userSessionControlId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
        pageable: Pageable
    ): Page<SessionAccessHistoryJpaEntity>
    
    fun findAllByOrderByOccurredAtDesc(pageable: Pageable): Page<SessionAccessHistoryJpaEntity>
}