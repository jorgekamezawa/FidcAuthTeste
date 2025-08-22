package com.banco.fidc.auth.domain.session.repository

import com.banco.fidc.auth.domain.session.entity.Session
import com.banco.fidc.auth.shared.dto.SessionFilter
import java.util.UUID

interface SessionRepository {
    fun save(session: Session): Session
    
    fun findBySessionId(sessionId: UUID): Session?
    
    fun findByCpfAndPartner(cpf: String, partner: String): Session?
    
    fun existsBySessionId(sessionId: UUID): Boolean
    
    fun deleteBySessionId(sessionId: UUID)
    
    fun findByFilter(filter: SessionFilter): List<Session>
    
    fun findActiveSessions(): List<Session>
}