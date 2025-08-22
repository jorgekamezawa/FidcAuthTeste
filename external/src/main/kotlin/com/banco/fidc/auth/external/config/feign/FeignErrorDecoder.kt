package com.banco.fidc.auth.external.config.feign

import feign.Response
import feign.codec.ErrorDecoder
import org.slf4j.LoggerFactory

class FeignErrorDecoder : ErrorDecoder {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    override fun decode(methodKey: String, response: Response): Exception {
        logger.debug("Feign error decoder: method=$methodKey, status=${response.status()}")
        
        return when (response.status()) {
            400 -> IllegalArgumentException("Bad Request: $methodKey")
            401 -> SecurityException("Unauthorized: $methodKey")
            403 -> SecurityException("Forbidden: $methodKey")
            404 -> NoSuchElementException("Not Found: $methodKey")
            429 -> IllegalStateException("Rate Limited: $methodKey")
            500, 502, 503, 504 -> RuntimeException("Server Error (${response.status()}): $methodKey")
            else -> RuntimeException("HTTP ${response.status()}: $methodKey")
        }
    }
}