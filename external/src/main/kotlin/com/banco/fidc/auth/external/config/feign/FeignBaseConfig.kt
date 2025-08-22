package com.banco.fidc.auth.external.config.feign

import feign.Retryer
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignBaseConfig {

    @Bean
    fun feignRetryer(): Retryer {
        // 100ms initial, 1s max, 3 attempts
        return Retryer.Default(100, 1000, 3)
    }

    @Bean
    fun feignErrorDecoder(): ErrorDecoder {
        return FeignErrorDecoder()
    }
}