package com.banco.fidc.auth.repository.jpa.session.impl

import com.banco.fidc.auth.domain.session.entity.SessionAccessHistory
import com.banco.fidc.auth.domain.session.repository.SessionAccessHistoryRepository
import com.banco.fidc.auth.repository.jpa.session.entity.toDomainEntity
import com.banco.fidc.auth.repository.jpa.session.entity.toJpaEntity
import com.banco.fidc.auth.repository.jpa.session.exception.SessionRepositoryException
import com.banco.fidc.auth.repository.jpa.session.repository.SessionAccessHistoryJpaRepository
import com.banco.fidc.auth.shared.dto.Page
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.net.InetAddress
import java.time.LocalDate
import java.util.UUID

@Repository
class SessionAccessHistoryRepositoryImpl(
    private val jpaRepository: SessionAccessHistoryJpaRepository
) : SessionAccessHistoryRepository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun save(sessionAccessHistory: SessionAccessHistory): SessionAccessHistory {
        logger.debug("Saving SessionAccessHistory: id=${sessionAccessHistory.id}, sessionId=${sessionAccessHistory.sessionId}")
        
        try {
            val jpaEntity = if (sessionAccessHistory.id > 0) {
                // Update existing (rare case for this entity)
                val existing = jpaRepository.findById(sessionAccessHistory.id)
                    .orElseThrow { 
                        IllegalStateException("SessionAccessHistory not found for update: id=${sessionAccessHistory.id}")
                    }
                sessionAccessHistory.toJpaEntity().apply { 
                    this.id = existing.id // Preserve ID
                }
            } else {
                // Create new (most common case)
                sessionAccessHistory.toJpaEntity()
            }
            
            val saved = jpaRepository.save(jpaEntity)
            logger.debug("SessionAccessHistory saved successfully: id=${saved.id}")
            
            return saved.toDomainEntity()
            
        } catch (e: IllegalStateException) {
            throw e // Business exceptions propagate unchanged
        } catch (e: Exception) {
            logger.error("Error saving SessionAccessHistory: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to save SessionAccessHistory",
                e
            )
        }
    }

    override fun findById(id: Long): SessionAccessHistory? {
        logger.debug("Finding SessionAccessHistory by id: $id")
        
        return try {
            jpaRepository.findById(id)
                .map { it.toDomainEntity() }
                .orElse(null)
        } catch (e: Exception) {
            logger.error("Error finding SessionAccessHistory by id: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find SessionAccessHistory by id",
                e
            )
        }
    }

    override fun findByExternalId(externalId: UUID): SessionAccessHistory? {
        logger.debug("Finding SessionAccessHistory by externalId: $externalId")
        
        return try {
            jpaRepository.findByUuid(externalId)
                ?.toDomainEntity()
        } catch (e: Exception) {
            logger.error("Error finding SessionAccessHistory by externalId: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find SessionAccessHistory by externalId",
                e
            )
        }
    }

    override fun findBySessionId(sessionId: UUID): List<SessionAccessHistory> {
        logger.debug("Finding SessionAccessHistory by sessionId: $sessionId")
        
        return try {
            jpaRepository.findBySessionId(sessionId)
                .map { it.toDomainEntity() }
        } catch (e: Exception) {
            logger.error("Error finding SessionAccessHistory by sessionId: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find SessionAccessHistory by sessionId",
                e
            )
        }
    }

    override fun findByUserSessionControlId(userSessionControlId: Long): List<SessionAccessHistory> {
        logger.debug("Finding SessionAccessHistory by userSessionControlId: $userSessionControlId")
        
        return try {
            jpaRepository.findByUserSessionControlIdOrderByOccurredAtDesc(userSessionControlId)
                .map { it.toDomainEntity() }
        } catch (e: Exception) {
            logger.error("Error finding SessionAccessHistory by userSessionControlId: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find SessionAccessHistory by userSessionControlId",
                e
            )
        }
    }

    override fun findLatestByUserSessionControlId(userSessionControlId: Long): SessionAccessHistory? {
        logger.debug("Finding latest SessionAccessHistory by userSessionControlId: $userSessionControlId")
        
        return try {
            jpaRepository.findTopByUserSessionControlIdOrderByOccurredAtDesc(userSessionControlId)
                ?.toDomainEntity()
        } catch (e: Exception) {
            logger.error("Error finding latest SessionAccessHistory by userSessionControlId: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find latest SessionAccessHistory by userSessionControlId",
                e
            )
        }
    }

    override fun findByIpAddress(ipAddress: InetAddress): List<SessionAccessHistory> {
        logger.debug("Finding SessionAccessHistory by ipAddress: $ipAddress")
        
        return try {
            jpaRepository.findByIpAddress(ipAddress)
                .map { it.toDomainEntity() }
        } catch (e: Exception) {
            logger.error("Error finding SessionAccessHistory by ipAddress: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find SessionAccessHistory by ipAddress",
                e
            )
        }
    }

    override fun findByDateRange(startDate: LocalDate, endDate: LocalDate): List<SessionAccessHistory> {
        logger.debug("Finding SessionAccessHistory by date range: $startDate to $endDate")
        
        return try {
            jpaRepository.findByDateRange(startDate, endDate)
                .map { it.toDomainEntity() }
        } catch (e: Exception) {
            logger.error("Error finding SessionAccessHistory by date range: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find SessionAccessHistory by date range",
                e
            )
        }
    }

    override fun findByUserSessionControlIdAndDateRange(
        userSessionControlId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): Page<SessionAccessHistory> {
        logger.debug("Finding SessionAccessHistory by userSessionControlId and date range: $userSessionControlId, $startDate to $endDate")
        
        try {
            val pageable = PageRequest.of(0, 100) // Default pagination
            
            val result = jpaRepository.findByUserSessionControlIdAndDateRange(
                userSessionControlId, startDate, endDate, pageable
            )
            
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
            logger.error("Error finding SessionAccessHistory by userSessionControlId and date range: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find SessionAccessHistory by userSessionControlId and date range",
                e
            )
        }
    }

    override fun findAll(
        page: Int,
        size: Int,
        sortBy: String,
        sortDirection: String
    ): Page<SessionAccessHistory> {
        logger.debug("Finding all SessionAccessHistory: page=$page, size=$size")
        
        try {
            val sort = Sort.by(
                if (sortDirection == "ASC") Sort.Direction.ASC else Sort.Direction.DESC,
                sortBy
            )
            val pageable = PageRequest.of(page, size, sort)
            
            val result = jpaRepository.findAllByOrderByOccurredAtDesc(pageable)
            
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
            logger.error("Error finding all SessionAccessHistory: ${e.message}", e)
            throw SessionRepositoryException(
                "Failed to find all SessionAccessHistory",
                e
            )
        }
    }
}