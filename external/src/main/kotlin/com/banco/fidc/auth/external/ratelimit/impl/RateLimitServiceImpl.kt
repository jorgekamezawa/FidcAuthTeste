package com.banco.fidc.auth.external.ratelimit.impl

import com.banco.fidc.auth.external.ratelimit.exception.RateLimitException
import com.banco.fidc.auth.usecase.session.service.RateLimitService
import com.banco.fidc.auth.usecase.session.dto.params.RateLimitCheckParams
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RateLimitServiceImpl(
    private val redisTemplate: RedisTemplate<String, String>
) : RateLimitService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun checkRateLimit(params: RateLimitCheckParams) {
        logger.debug("Checking rate limit: ip=${params.clientIpAddress}, userAgent=${params.userAgent.take(50)}...")
        checkRedisRateLimit(params)
    }
    
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
    
    private fun incrementAndGetCount(key: String, expiration: Duration): Long {
        val count = redisTemplate.opsForValue().increment(key) ?: 1L
        if (count == 1L) {
            redisTemplate.expire(key, expiration)
        }
        return count
    }
}