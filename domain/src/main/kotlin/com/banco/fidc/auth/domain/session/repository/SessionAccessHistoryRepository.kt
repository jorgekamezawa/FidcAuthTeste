package com.banco.fidc.auth.domain.session.repository

import com.banco.fidc.auth.domain.session.entity.SessionAccessHistory
import com.banco.fidc.auth.shared.dto.Page
import java.net.InetAddress
import java.time.LocalDate
import java.util.UUID

interface SessionAccessHistoryRepository {
    fun save(sessionAccessHistory: SessionAccessHistory): SessionAccessHistory
    
    fun findById(id: Long): SessionAccessHistory?
    
    fun findByExternalId(externalId: UUID): SessionAccessHistory?
    
    fun findBySessionId(sessionId: UUID): List<SessionAccessHistory>
    
    fun findByUserSessionControlId(userSessionControlId: Long): List<SessionAccessHistory>
    
    fun findLatestByUserSessionControlId(userSessionControlId: Long): SessionAccessHistory?
    
    fun findByIpAddress(ipAddress: InetAddress): List<SessionAccessHistory>
    
    fun findByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<SessionAccessHistory>
    
    fun findByUserSessionControlIdAndDateRange(
        userSessionControlId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): Page<SessionAccessHistory>
    
    fun findAll(
        page: Int,
        size: Int,
        sortBy: String = "occurredAt",
        sortDirection: String = "DESC"
    ): Page<SessionAccessHistory>
}