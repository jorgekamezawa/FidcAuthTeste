package com.banco.fidc.auth.external.ratelimit.impl

import com.banco.fidc.auth.external.ratelimit.exception.RateLimitException
import com.banco.fidc.auth.usecase.session.service.RateLimitService
import com.banco.fidc.auth.usecase.session.dto.params.RateLimitCheckParams
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Service
class RateLimitServiceImpl(
    private val redisTemplate: RedisTemplate<String, String>
) : RateLimitService {

    private val logger = LoggerFactory.getLogger(this::class.java)
    
    // MOCK: In-memory counter para quando Redis não estiver disponível
    private val inMemoryCounters = ConcurrentHashMap<String, RateLimitCounter>()

    override fun checkRateLimit(params: RateLimitCheckParams) {
        logger.debug("Checking rate limit: ip=${params.clientIpAddress}, userAgent=${params.userAgent.take(50)}...")
        
        // TODO: QUANDO REDIS ESTIVER ESTÁVEL, DESCOMENTE E USE APENAS REDIS
        // checkRedisRateLimit(params)
        
        // MOCK: USANDO CONTADOR EM MEMÓRIA PARA DESENVOLVIMENTO  
        checkInMemoryRateLimit(params)
    }
    
    // MÉTODO REAL PARA QUANDO REDIS ESTIVER ESTÁVEL
    private fun checkRedisRateLimit(params: RateLimitCheckParams) {
        val ipKey = "rate_limit:ip:${params.clientIpAddress}"
        val userAgentKey = "rate_limit:ua:${params.userAgent.hashCode()}"
        
        try {
            // Rate limit por IP: 20 req/min
            val ipCount = incrementAndGetCount(ipKey, Duration.ofMinutes(1))
            if (ipCount > 20) {
                logger.warn("Rate limit exceeded for IP: ${params.clientIpAddress}")
                throw RateLimitException("Rate limit exceeded for IP address")
            }
            
            // Rate limit por User-Agent: 40 req/min
            val uaCount = incrementAndGetCount(userAgentKey, Duration.ofMinutes(1))
            if (uaCount > 40) {
                logger.warn("Rate limit exceeded for User-Agent: ${params.userAgent.take(50)}")
                throw RateLimitException("Rate limit exceeded for User-Agent")
            }
            
            logger.debug("Rate limit check passed: ip=$ipCount/20, ua=$uaCount/40")
            
        } catch (e: RateLimitException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error checking rate limit on Redis: ${e.message}", e)
            // Fallback para permitir request se Redis falhar
            logger.warn("Rate limit check failed - allowing request due to Redis error")
        }
    }
    
    // MOCK: CONTADOR EM MEMÓRIA PARA DESENVOLVIMENTO
    private fun checkInMemoryRateLimit(params: RateLimitCheckParams) {
        logger.warn("USANDO RATE LIMIT EM MEMÓRIA - Redis não disponível ainda")
        
        val now = System.currentTimeMillis()
        val windowMs = 60_000L // 1 minuto
        
        // Rate limit por IP: 20 req/min
        val ipKey = "ip:${params.clientIpAddress}"
        val ipCounter = inMemoryCounters.computeIfAbsent(ipKey) { RateLimitCounter() }
        val ipCount = ipCounter.incrementAndGet(now, windowMs)
        
        if (ipCount > 20) {
            logger.warn("Rate limit exceeded for IP: ${params.clientIpAddress}")
            throw RateLimitException("Rate limit exceeded for IP address")
        }
        
        // Rate limit por User-Agent: 40 req/min
        val uaKey = "ua:${params.userAgent.hashCode()}"
        val uaCounter = inMemoryCounters.computeIfAbsent(uaKey) { RateLimitCounter() }
        val uaCount = uaCounter.incrementAndGet(now, windowMs)
        
        if (uaCount > 40) {
            logger.warn("Rate limit exceeded for User-Agent: ${params.userAgent.take(50)}")
            throw RateLimitException("Rate limit exceeded for User-Agent")
        }
        
        logger.debug("Rate limit check passed: ip=$ipCount/20, ua=$uaCount/40")
        
        // Cleanup periódico de contadores antigos
        if (now % 30_000 == 0L) { // A cada 30 segundos
            cleanupOldCounters(now, windowMs)
        }
    }
    
    private fun incrementAndGetCount(key: String, expiration: Duration): Long {
        val count = redisTemplate.opsForValue().increment(key) ?: 1L
        if (count == 1L) {
            redisTemplate.expire(key, expiration)
        }
        return count
    }
    
    private fun cleanupOldCounters(now: Long, windowMs: Long) {
        val cutoff = now - windowMs
        inMemoryCounters.entries.removeIf { (_, counter) ->
            counter.lastAccess < cutoff
        }
        logger.debug("Cleaned up old rate limit counters")
    }
    
    // MOCK: Contador em memória simples
    private data class RateLimitCounter(
        var count: Long = 0,
        var windowStart: Long = 0,
        var lastAccess: Long = System.currentTimeMillis()
    ) {
        fun incrementAndGet(now: Long, windowMs: Long): Long {
            lastAccess = now
            
            // Se estamos em uma nova janela, reinicia o contador
            if (now - windowStart > windowMs) {
                windowStart = now
                count = 1
            } else {
                count++
            }
            
            return count
        }
    }
}