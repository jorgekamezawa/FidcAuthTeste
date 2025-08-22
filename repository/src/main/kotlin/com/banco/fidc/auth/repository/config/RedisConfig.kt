package com.banco.fidc.auth.repository.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    @Primary
    fun redisObjectMapper(): ObjectMapper {
        return jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Bean
    fun redisTemplate(
        connectionFactory: RedisConnectionFactory,
        redisObjectMapper: ObjectMapper
    ): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        // JSON serialization for interoperability
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = createJsonSerializer(redisObjectMapper)
        template.hashKeySerializer = StringRedisSerializer()
        template.hashValueSerializer = createJsonSerializer(redisObjectMapper)

        template.afterPropertiesSet()
        return template
    }

    private fun createJsonSerializer(objectMapper: ObjectMapper): Jackson2JsonRedisSerializer<Any> {
        return Jackson2JsonRedisSerializer(objectMapper, Any::class.java)
    }
}