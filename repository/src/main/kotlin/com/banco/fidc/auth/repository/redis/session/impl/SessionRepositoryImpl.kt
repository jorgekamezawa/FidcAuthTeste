package com.banco.fidc.auth.repository.redis.session.impl

import com.banco.fidc.auth.domain.session.entity.Session
import com.banco.fidc.auth.domain.session.repository.SessionRepository
import com.banco.fidc.auth.repository.redis.session.entity.SessionRedisEntity
import com.banco.fidc.auth.repository.redis.session.entity.toDomainEntity
import com.banco.fidc.auth.repository.redis.session.entity.toRedisEntity
import com.banco.fidc.auth.repository.redis.session.exception.RedisRepositoryException
import com.banco.fidc.auth.shared.dto.SessionFilter
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.util.UUID
import java.util.concurrent.TimeUnit

@Repository
class SessionRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper
) : SessionRepository {

    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
         private const val SESSION_KEY_PREFIX = "fidc:session"
        private const val CPF_INDEX_KEY_PREFIX = "fidc:cpf_index"
    }

    override fun save(session: Session): Session {
        val key = generateSessionKey(session.partner, session.sessionId)
        val redisEntity = session.toRedisEntity()
        
        logger.debug("Saving session to Redis: key=$key")
        
        try {
            // Save session data
            redisTemplate.opsForValue().set(
                key, 
                redisEntity, 
                session.ttlMinutes.toLong(), 
                TimeUnit.MINUTES
            )
            
            // Create CPF index for faster lookups
            createCpfIndex(session.userInfo.cpf, session.partner, session.sessionId, session.ttlMinutes.toLong())
            
            logger.debug("Session saved to Redis successfully: sessionId=${session.sessionId}")
            return session
            
        } catch (e: Exception) {
            logger.error("Error saving session to Redis: key=$key", e)
            throw RedisRepositoryException(
                "Failed to save session to Redis",
                e
            )
        }
    }

    override fun update(session: Session): Session {
        val key = generateSessionKey(session.partner, session.sessionId)
        val redisEntity = session.toRedisEntity()
        
        logger.debug("Updating session in Redis: key=$key")
        
        try {
            // Buscar TTL atual
            val currentTtl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS)
            
            // Validar que a sessão ainda tem TTL válido
            if (currentTtl <= 0) {
                logger.error("Cannot update session without valid TTL: sessionId=${session.sessionId}, ttl=$currentTtl")
                throw RedisRepositoryException(
                    "Session not found or expired - cannot update session without valid TTL"
                )
            }
            
            // Atualizar dados
            redisTemplate.opsForValue().set(key, redisEntity)
            
            // Restaurar TTL original
            redisTemplate.expire(key, currentTtl, TimeUnit.MILLISECONDS)
            
            // Update CPF index with same TTL
            val ttlMinutes = currentTtl / (1000 * 60) // Convert milliseconds to minutes
            createCpfIndex(session.userInfo.cpf, session.partner, session.sessionId, ttlMinutes)
            
            logger.debug("Session updated preserving TTL: ${currentTtl}ms")
            
            logger.debug("Session updated in Redis successfully: sessionId=${session.sessionId}")
            return session
            
        } catch (e: RedisRepositoryException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error updating session in Redis: key=$key", e)
            throw RedisRepositoryException(
                "Failed to update session in Redis",
                e
            )
        }
    }

    override fun findBySessionId(sessionId: UUID): Session? {
        try {
            logger.debug("Finding session by sessionId: $sessionId")
            
            // Since we don't have the partner, we need to scan for the sessionId
            val pattern = "$SESSION_KEY_PREFIX:*:$sessionId"
            val keys = redisTemplate.keys(pattern)
            
            if (keys.isEmpty()) {
                return null
            }
            
            // Should only have one key matching this pattern
            val key = keys.first()
            val value = redisTemplate.opsForValue().get(key) ?: return null
            
            // Handle both direct object and Map (JSON) deserialization
            val redisEntity = when (value) {
                is SessionRedisEntity -> value
                is Map<*, *> -> deserializeFromMap(value, key)
                else -> {
                    logger.warn("Unexpected type in Redis: key=$key, type=${value::class.java}")
                    null
                }
            }
            
            return redisEntity?.toDomainEntity()
            
        } catch (e: Exception) {
            logger.error("Error retrieving session from Redis: sessionId=$sessionId", e)
            throw RedisRepositoryException(
                "Failed to retrieve session from Redis",
                e
            )
        }
    }

    override fun findByCpfAndPartner(cpf: String, partner: String): Session? {
        logger.debug("Finding session by cpf and partner using index: cpf=${cpf.take(3)}***, partner=$partner")
        
        try {
            val indexKey = generateCpfIndexKey(cpf, partner)
            val sessionId = redisTemplate.opsForValue().get(indexKey) as? String
            
            if (sessionId != null) {
                val sessionUuid = UUID.fromString(sessionId)
                // Now we can make a direct lookup since we have both partner and sessionId
                val sessionKey = generateSessionKey(partner, sessionUuid)
                val value = redisTemplate.opsForValue().get(sessionKey)
                
                if (value != null) {
                    val redisEntity = when (value) {
                        is SessionRedisEntity -> value
                        is Map<*, *> -> deserializeFromMap(value, sessionKey)
                        else -> {
                            logger.warn("Unexpected type in Redis: key=$sessionKey, type=${value::class.java}")
                            null
                        }
                    }
                    
                    return redisEntity?.toDomainEntity()
                }
            }
            
            return null
            
        } catch (e: Exception) {
            logger.error("Error finding session by cpf and partner using index", e)
            throw RedisRepositoryException(
                "Failed to find session by cpf and partner using index",
                e
            )
        }
    }

    override fun existsBySessionId(sessionId: UUID): Boolean {
        return try {
            logger.debug("Checking session existence: sessionId=$sessionId")
            
            val pattern = "$SESSION_KEY_PREFIX:*:$sessionId"
            val keys = redisTemplate.keys(pattern)
            
            keys.isNotEmpty()
            
        } catch (e: Exception) {
            logger.error("Error checking session existence: sessionId=$sessionId", e)
            throw RedisRepositoryException(
                "Failed to check session existence",
                e
            )
        }
    }

    override fun deleteBySessionId(sessionId: UUID) {
        try {
            logger.debug("Deleting session from Redis: sessionId=$sessionId")
            
            // Get session first to clean up the index
            val session = findBySessionId(sessionId)
            
            if (session != null) {
                val key = generateSessionKey(session.partner, session.sessionId)
                val deleted = redisTemplate.delete(key)
                
                if (deleted) {
                    // Clean up CPF index
                    removeCpfIndex(session.userInfo.cpf, session.partner)
                    logger.debug("Session deleted from Redis successfully: sessionId=$sessionId")
                } else {
                    logger.warn("Session not found for deletion: sessionId=$sessionId")
                }
            } else {
                logger.warn("Session not found for deletion: sessionId=$sessionId")
            }
            
        } catch (e: Exception) {
            logger.error("Error deleting session from Redis: sessionId=$sessionId", e)
            throw RedisRepositoryException(
                "Failed to delete session from Redis",
                e
            )
        }
    }

    override fun findByFilter(filter: SessionFilter): List<Session> {
        logger.debug("Finding sessions by filter: $filter")
        
        try {
            val pattern = "$SESSION_KEY_PREFIX:*:*"
            val keys = redisTemplate.keys(pattern)
            val sessions = mutableListOf<Session>()
            
            for (key in keys) {
                try {
                    val value = redisTemplate.opsForValue().get(key) ?: continue
                    
                    val redisEntity = when (value) {
                        is SessionRedisEntity -> value
                        is Map<*, *> -> deserializeFromMap(value, key)
                        else -> continue
                    }
                    
                    if (redisEntity != null) {
                        val session = redisEntity.toDomainEntity()
                        
                        // Apply filters
                        var matches = true
                        
                        if (filter.cpf != null && session.userInfo.cpf != filter.cpf) {
                            matches = false
                        }
                        
                        if (filter.partner != null && !session.partner.equals(filter.partner, ignoreCase = true)) {
                            matches = false
                        }
                        
                        if (filter.channel != null && session.channel.name != filter.channel) {
                            matches = false
                        }
                        
                        if (filter.createdFrom != null && session.createdAt.toLocalDate().isBefore(filter.createdFrom)) {
                            matches = false
                        }
                        
                        if (filter.createdTo != null && session.createdAt.toLocalDate().isAfter(filter.createdTo)) {
                            matches = false
                        }
                        
                        if (matches) {
                            sessions.add(session)
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Error processing session during filter: $key", e)
                    // Continue with next key
                }
            }
            
            return sessions.sortedByDescending { it.createdAt }
            
        } catch (e: Exception) {
            logger.error("Error finding sessions by filter", e)
            throw RedisRepositoryException(
                "Failed to find sessions by filter",
                e
            )
        }
    }

    override fun findActiveSessions(): List<Session> {
        logger.debug("Finding all active sessions")
        
        try {
            val pattern = "$SESSION_KEY_PREFIX:*:*"
            val keys = redisTemplate.keys(pattern)
            val sessions = mutableListOf<Session>()
            
            for (key in keys) {
                try {
                    val value = redisTemplate.opsForValue().get(key) ?: continue
                    
                    val redisEntity = when (value) {
                        is SessionRedisEntity -> value
                        is Map<*, *> -> deserializeFromMap(value, key)
                        else -> continue
                    }
                    
                    redisEntity?.let { 
                        sessions.add(it.toDomainEntity())
                    }
                } catch (e: Exception) {
                    logger.warn("Error processing session key: $key", e)
                    // Continue with next key
                }
            }
            
            return sessions.sortedByDescending { it.createdAt }
            
        } catch (e: Exception) {
            logger.error("Error finding active sessions", e)
            throw RedisRepositoryException(
                "Failed to find active sessions",
                e
            )
        }
    }

    private fun generateSessionKey(partner: String, sessionId: UUID): String {
        return "$SESSION_KEY_PREFIX:${partner.lowercase()}:$sessionId"
    }
    
    private fun generateCpfIndexKey(cpf: String, partner: String): String {
        return "$CPF_INDEX_KEY_PREFIX:${cpf}:${partner.lowercase()}"
    }
    
    private fun createCpfIndex(cpf: String, partner: String, sessionId: UUID, ttlMinutes: Long) {
        try {
            val indexKey = generateCpfIndexKey(cpf, partner)
            redisTemplate.opsForValue().set(indexKey, sessionId.toString(), ttlMinutes, TimeUnit.MINUTES)
            logger.debug("CPF index created: cpf=${cpf.take(3)}***, partner=$partner, sessionId=$sessionId")
        } catch (e: Exception) {
            logger.warn("Error creating CPF index: cpf=${cpf.take(3)}***, partner=$partner", e)
            // Not throwing exception as this is an optimization, not critical functionality
        }
    }
    
    private fun removeCpfIndex(cpf: String, partner: String) {
        try {
            val indexKey = generateCpfIndexKey(cpf, partner)
            redisTemplate.delete(indexKey)
            logger.debug("CPF index removed: cpf=${cpf.take(3)}***, partner=$partner")
        } catch (e: Exception) {
            logger.warn("Error removing CPF index: cpf=${cpf.take(3)}***, partner=$partner", e)
            // Not throwing exception as this is cleanup, not critical functionality
        }
    }

    private fun deserializeFromMap(value: Map<*, *>, key: String): SessionRedisEntity? {
        return try {
            val json = objectMapper.writeValueAsString(value)
            objectMapper.readValue(json, SessionRedisEntity::class.java)
        } catch (e: Exception) {
            logger.error("Error deserializing session from Redis: key=$key", e)
            null
        }
    }
}