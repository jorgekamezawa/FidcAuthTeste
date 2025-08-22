package com.banco.fidc.auth.application.config

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.stereotype.Component

@Component
class RedisHealthIndicator(
    private val redisConnectionFactory: RedisConnectionFactory
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val connection = redisConnectionFactory.connection
            connection.ping()
            connection.close()
            Health.up()
                .withDetail("type", "Redis")
                .build()
        } catch (ex: Exception) {
            Health.down()
                .withDetail("type", "Redis")
                .withException(ex)
                .build()
        }
    }
}