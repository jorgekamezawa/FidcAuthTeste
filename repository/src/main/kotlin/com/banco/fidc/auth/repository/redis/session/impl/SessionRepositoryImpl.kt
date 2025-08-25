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
        private const val SESSION_KEY_PREFIX = "session"
        private const val DEFAULT_TTL_MINUTES = 30L
    }

    override fun save(session: Session): Session {
        val key = generateSessionKey(session.sessionId)
        val redisEntity = session.toRedisEntity()
        
        logger.debug("Saving session to Redis: key=$key")
        
        try {
            redisTemplate.opsForValue().set(
                key, 
                redisEntity, 
                DEFAULT_TTL_MINUTES, 
                TimeUnit.MINUTES
            )
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
        val key = generateSessionKey(session.sessionId)
        val redisEntity = session.toRedisEntity()
        
        logger.debug("Updating session in Redis: key=$key")
        
        try {
            // Buscar TTL atual
            val currentTtl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS)
            
            // Atualizar dados
            redisTemplate.opsForValue().set(key, redisEntity)
            
            // Restaurar TTL original se ainda existir
            if (currentTtl > 0) {
                redisTemplate.expire(key, currentTtl, TimeUnit.MILLISECONDS)
                logger.debug("Session updated preserving TTL: ${currentTtl}ms")
            } else {
                // Fallback para TTL padrão se não conseguir obter TTL atual
                redisTemplate.expire(key, DEFAULT_TTL_MINUTES, TimeUnit.MINUTES)
                logger.warn("Could not preserve TTL, using default: ${DEFAULT_TTL_MINUTES}min")
            }
            
            logger.debug("Session updated in Redis successfully: sessionId=${session.sessionId}")
            return session
            
        } catch (e: Exception) {
            logger.error("Error updating session in Redis: key=$key", e)
            throw RedisRepositoryException(
                "Failed to update session in Redis",
                e
            )
        }
    }

    override fun findBySessionId(sessionId: UUID): Session? {
        val key = generateSessionKey(sessionId)
        
        try {
            logger.debug("Finding session by sessionId: $sessionId")
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
            logger.error("Error retrieving session from Redis: key=$key", e)
            throw RedisRepositoryException(
                "Failed to retrieve session from Redis",
                e
            )
        }
    }

    override fun findByCpfAndPartner(cpf: String, partner: String): Session? {
        logger.debug("Finding session by cpf and partner: cpf=${cpf.take(3)}***, partner=$partner")
        
        try {
            // Pattern to scan for sessions with specific cpf and partner
            val pattern = "$SESSION_KEY_PREFIX:*"
            val keys = redisTemplate.keys(pattern)
            
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
                        if (session.userInfo.cpf == cpf && session.partner == partner) {
                            return session
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Error processing session key during scan: $key", e)
                    // Continue with next key
                }
            }
            
            return null
            
        } catch (e: Exception) {
            logger.error("Error finding session by cpf and partner", e)
            throw RedisRepositoryException(
                "Failed to find session by cpf and partner",
                e
            )
        }
    }

    override fun existsBySessionId(sessionId: UUID): Boolean {
        val key = generateSessionKey(sessionId)
        
        return try {
            redisTemplate.hasKey(key)
        } catch (e: Exception) {
            logger.error("Error checking session existence: sessionId=$sessionId", e)
            throw RedisRepositoryException(
                "Failed to check session existence",
                e
            )
        }
    }

    override fun deleteBySessionId(sessionId: UUID) {
        val key = generateSessionKey(sessionId)
        
        try {
            logger.debug("Deleting session from Redis: sessionId=$sessionId")
            val deleted = redisTemplate.delete(key)
            if (deleted) {
                logger.debug("Session deleted from Redis successfully: sessionId=$sessionId")
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
            val pattern = "$SESSION_KEY_PREFIX:*"
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
                        
                        if (filter.partner != null && session.partner != filter.partner) {
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
            val pattern = "$SESSION_KEY_PREFIX:*"
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

    private fun generateSessionKey(sessionId: UUID): String {
        return "$SESSION_KEY_PREFIX:$sessionId"
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