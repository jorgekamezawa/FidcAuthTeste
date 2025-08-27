package com.banco.fidc.auth.repository.jpa.session.impl

import com.banco.fidc.auth.domain.session.entity.UserSessionControl
import com.banco.fidc.auth.domain.session.repository.UserSessionControlRepository
import com.banco.fidc.auth.repository.jpa.session.entity.toDomainEntity
import com.banco.fidc.auth.repository.jpa.session.entity.toJpaEntity
import com.banco.fidc.auth.repository.jpa.session.exception.SessionRepositoryException
import com.banco.fidc.auth.repository.jpa.session.repository.UserSessionControlJpaRepository
import com.banco.fidc.auth.shared.dto.UserSessionControlFilter
import com.banco.fidc.auth.shared.dto.Page
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Repository
class UserSessionControlRepositoryImpl(
    private val jpaRepository: UserSessionControlJpaRepository
) : UserSessionControlRepository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun save(userSessionControl: UserSessionControl): UserSessionControl {
        logger.debug("Saving UserSessionControl: id=${userSessionControl.id}, externalId=${userSessionControl.externalId}")
        
        try {
            val jpaEntity = if (userSessionControl.id > 0) {
                // Update existing
                val existing = jpaRepository.findById(userSessionControl.id)
                    .orElseThrow { 
                        IllegalStateException("UserSessionControl not found for update: id=${userSessionControl.id}")
                    }
                userSessionControl.toJpaEntity().apply { 
                    this.id = existing.id // Preserve ID
                }
            } else {
                // Create new
                userSessionControl.toJpaEntity()
            }
            
            val saved = jpaRepository.save(jpaEntity)
            logger.debug("UserSessionControl saved successfully: id=${saved.id}")
            
            return saved.toDomainEntity()
            
        } catch (e: IllegalStateException) {
            throw e // Business exceptions propagate unchanged
        } catch (e: Exception) {
            logger.error("Error saving UserSessionControl: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to save UserSessionControl",
                e
            )
        }
    }

    override fun findById(id: Long): UserSessionControl? {
        logger.debug("Finding UserSessionControl by id: $id")
        
        return try {
            jpaRepository.findById(id)
                .map { it.toDomainEntity() }
                .orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding UserSessionControl by id: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find UserSessionControl by id",
                e
            )
        }
    }

    override fun findByExternalId(externalId: UUID): UserSessionControl? {
        logger.debug("Finding UserSessionControl by externalId: $externalId")
        
        return try {
            jpaRepository.findByUuid(externalId)
                ?.toDomainEntity()
        } catch (e: Exception) {
            logger.error("Error finding UserSessionControl by externalId: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find UserSessionControl by externalId",
                e
            )
        }
    }

    override fun findByCpfAndPartner(cpf: String, partner: String): UserSessionControl? {
        logger.debug("Finding UserSessionControl by cpf and partner: cpf=${cpf.take(3)}***, partner=$partner")
        
        return try {
            jpaRepository.findByCpfAndPartner(cpf, partner)
                ?.toDomainEntity()
        } catch (e: Exception) {
            logger.error("Error finding UserSessionControl by cpf and partner: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find UserSessionControl by cpf and partner",
                e
            )
        }
    }

    override fun findByCurrentSessionId(currentSessionId: UUID): UserSessionControl? {
        logger.debug("Finding UserSessionControl by currentSessionId: $currentSessionId")
        
        return try {
            jpaRepository.findByCurrentSessionId(currentSessionId)
                ?.toDomainEntity()
        } catch (e: Exception) {
            logger.error("Error finding UserSessionControl by currentSessionId: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find UserSessionControl by currentSessionId",
                e
            )
        }
    }

    override fun existsById(id: Long): Boolean {
        return try {
            jpaRepository.existsById(id)
        } catch (e: Exception) {
            logger.error("Error checking existence of UserSessionControl by id: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to check UserSessionControl existence by id",
                e
            )
        }
    }

    override fun existsByExternalId(externalId: UUID): Boolean {
        return try {
            jpaRepository.existsByUuid(externalId)
        } catch (e: Exception) {
            logger.error("Error checking existence of UserSessionControl by externalId: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to check UserSessionControl existence by externalId",
                e
            )
        }
    }

    override fun existsByCpfAndPartner(cpf: String, partner: String): Boolean {
        return try {
            jpaRepository.existsByCpfAndPartner(cpf, partner)
        } catch (e: Exception) {
            logger.error("Error checking existence of UserSessionControl by cpf and partner: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to check UserSessionControl existence by cpf and partner",
                e
            )
        }
    }

    override fun findActiveByCpf(cpf: String): List<UserSessionControl> {
        logger.debug("Finding active UserSessionControls by cpf: ${cpf.take(3)}***")
        
        return try {
            jpaRepository.findByCpfAndIsActiveTrue(cpf)
                .map { it.toDomainEntity() }
        } catch (e: Exception) {
            logger.error("Error finding active UserSessionControls by cpf: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find active UserSessionControls by cpf",
                e
            )
        }
    }

    override fun findByFilter(filter: UserSessionControlFilter): List<UserSessionControl> {
        logger.debug("Finding UserSessionControls by filter: $filter")
        
        return try {
            val firstAccessFrom = filter.firstAccessFrom?.atStartOfDay()
            val firstAccessTo = filter.firstAccessTo?.atTime(LocalTime.MAX)
            val lastAccessFrom = filter.lastAccessFrom?.atStartOfDay()
            val lastAccessTo = filter.lastAccessTo?.atTime(LocalTime.MAX)
            
            jpaRepository.findByFilter(
                cpf = filter.cpf,
                partner = filter.partner,
                isActive = filter.isActive,
                firstAccessFrom = firstAccessFrom,
                firstAccessTo = firstAccessTo,
                lastAccessFrom = lastAccessFrom,
                lastAccessTo = lastAccessTo
            ).map { it.toDomainEntity() }
        } catch (e: Exception) {
            logger.error("Error finding UserSessionControls by filter: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find UserSessionControls by filter",
                e
            )
        }
    }

    override fun findAllActive(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): Page<UserSessionControl> {
        logger.debug("Finding active UserSessionControls: page=$page, size=$size")
        
        try {
            val sort = Sort.by(
                if (sortDirection == "ASC") Sort.Direction.ASC else Sort.Direction.DESC,
                sortBy
            )
            val pageable = PageRequest.of(page, size, sort)
            
            val result = jpaRepository.findByIsActiveTrue(pageable)
            
            return Page(
                content = result.content.map { it.toDomainEntity() },
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                currentPage = result.number,
                pageSize = result.size,
                hasNext = result.hasNext(),
                hasPrevious = result.hasPrevious()
            )
        } catch (e: Exception) {
            logger.error("Error finding active UserSessionControls: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find active UserSessionControls",
                e
            )
        }
    }
}