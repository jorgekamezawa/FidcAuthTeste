package com.banco.fidc.auth.domain.session.repository

import com.banco.fidc.auth.domain.session.entity.UserSessionControl
import com.banco.fidc.auth.shared.dto.UserSessionControlFilter
import com.banco.fidc.auth.shared.dto.Page
import java.util.UUID

interface UserSessionControlRepository {
    fun save(userSessionControl: UserSessionControl): UserSessionControl
    
    fun findById(id: Long): UserSessionControl?
    
    fun findByExternalId(externalId: UUID): UserSessionControl?
    
    fun findByCpfAndPartner(cpf: String, partner: String): UserSessionControl?
    
    fun existsById(id: Long): Boolean
    
    fun existsByExternalId(externalId: UUID): Boolean
    
    fun existsByCpfAndPartner(cpf: String, partner: String): Boolean
    
    fun findActiveByCpf(cpf: String): List<UserSessionControl>
    
    fun findByFilter(filter: UserSessionControlFilter): List<UserSessionControl>
    
    fun findAllActive(
        page: Int,
        size: Int,
        sortBy: String = "lastAccessAt",
        sortDirection: String = "DESC"
    ): Page<UserSessionControl>
}