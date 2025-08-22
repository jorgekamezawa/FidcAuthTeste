package com.banco.fidc.auth.application.config

import com.banco.fidc.auth.usecase.session.configprovider.SessionConfigProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SessionConfigProviderImpl(
    @Value("\${properties.session.ttl-minutes}")
    private val ttlMinutes: Int,
    
    @Value("\${properties.session.cleanup-interval-minutes}")
    private val cleanupIntervalMinutes: Int,
    
    @Value("\${properties.session.secret-length}")
    private val secretLength: Int
) : SessionConfigProvider {
    
    override fun getTtlMinutes(): Int = ttlMinutes
    
    override fun getCleanupIntervalMinutes(): Int = cleanupIntervalMinutes
    
    override fun getSecretLength(): Int = secretLength
}