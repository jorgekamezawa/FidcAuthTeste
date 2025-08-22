package com.banco.fidc.auth.application.config.properties

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

@Validated
@Configuration
@ConfigurationProperties(prefix = "properties.session")
data class SessionProperties(
    @field:Min(value = 1, message = "ttlMinutes must be greater than 0")
    var ttlMinutes: Int = 30,
    
    @field:Min(value = 1, message = "cleanupIntervalMinutes must be greater than 0")
    var cleanupIntervalMinutes: Int = 5
)