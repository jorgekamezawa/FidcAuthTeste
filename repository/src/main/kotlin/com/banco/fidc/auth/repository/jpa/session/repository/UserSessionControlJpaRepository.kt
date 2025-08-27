package com.banco.fidc.auth.repository.jpa.session.repository

import com.banco.fidc.auth.repository.jpa.session.entity.UserSessionControlJpaEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserSessionControlJpaRepository : JpaRepository<UserSessionControlJpaEntity, Long> {
    
    fun findByUuid(uuid: UUID): UserSessionControlJpaEntity?
    
    fun existsByUuid(uuid: UUID): Boolean
    
    @Query("SELECT u FROM UserSessionControlJpaEntity u WHERE u.cpf = :cpf AND LOWER(u.partner) = LOWER(:partner)")
    fun findByCpfAndPartner(@Param("cpf") cpf: String, @Param("partner") partner: String): UserSessionControlJpaEntity?
    
    @Query("SELECT COUNT(u) > 0 FROM UserSessionControlJpaEntity u WHERE u.cpf = :cpf AND LOWER(u.partner) = LOWER(:partner)")
    fun existsByCpfAndPartner(@Param("cpf") cpf: String, @Param("partner") partner: String): Boolean
    
    fun findByCpfAndIsActiveTrue(cpf: String): List<UserSessionControlJpaEntity>
    
    fun findByIsActiveTrue(): List<UserSessionControlJpaEntity>
    
    fun findByIsActiveTrue(pageable: Pageable): Page<UserSessionControlJpaEntity>
    
    @Query("""
        SELECT u FROM UserSessionControlJpaEntity u 
        WHERE (:cpf IS NULL OR u.cpf = :cpf)
        AND (:partner IS NULL OR LOWER(u.partner) = LOWER(:partner))
        AND (:isActive IS NULL OR u.isActive = :isActive)
        AND (:firstAccessFrom IS NULL OR u.firstAccessAt >= :firstAccessFrom)
        AND (:firstAccessTo IS NULL OR u.firstAccessAt <= :firstAccessTo)
        AND (:lastAccessFrom IS NULL OR u.lastAccessAt >= :lastAccessFrom)
        AND (:lastAccessTo IS NULL OR u.lastAccessAt <= :lastAccessTo)
        ORDER BY u.lastAccessAt DESC
    """)
    fun findByFilter(
        @Param("cpf") cpf: String?,
        @Param("partner") partner: String?,
        @Param("isActive") isActive: Boolean?,
        @Param("firstAccessFrom") firstAccessFrom: java.time.LocalDateTime?,
        @Param("firstAccessTo") firstAccessTo: java.time.LocalDateTime?,
        @Param("lastAccessFrom") lastAccessFrom: java.time.LocalDateTime?,
        @Param("lastAccessTo") lastAccessTo: java.time.LocalDateTime?
    ): List<UserSessionControlJpaEntity>
}