package com.banco.fidc.auth.application.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.util.TimeZone

@Configuration
class TimeZoneConfig {

    companion object {
        private val log = LoggerFactory.getLogger(TimeZoneConfig::class.java)
        private const val BRAZIL_TIMEZONE = "America/Sao_Paulo"
    }

    @PostConstruct
    fun init() {
        val timezone = TimeZone.getTimeZone(BRAZIL_TIMEZONE)
        TimeZone.setDefault(timezone)
        
        check(TimeZone.getDefault().id == BRAZIL_TIMEZONE) {
            "Failed to configure timezone. Expected: $BRAZIL_TIMEZONE, Current: ${TimeZone.getDefault().id}"
        }
        
        log.info("Timezone configured to: $BRAZIL_TIMEZONE (${timezone.displayName})")
    }
}