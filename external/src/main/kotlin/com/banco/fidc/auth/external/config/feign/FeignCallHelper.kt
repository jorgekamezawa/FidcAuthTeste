package com.banco.fidc.auth.external.config.feign

import com.banco.fidc.auth.shared.exception.InfrastructureException
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FeignCallHelper {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun <T> executeFeignCall(
        componentName: String,
        operation: String,
        call: () -> T?,
        createException: (String, Throwable?) -> InfrastructureException,
        handle404AsNull: Boolean = false
    ): T? {
        logger.debug("Executing Feign call: component=$componentName, operation=$operation")

        return try {
            call()
        } catch (e: FeignException.NotFound) {
            if (handle404AsNull) {
                logger.debug("$componentName returned 404 - treating as null")
                null
            } else {
                throw createException("Resource not found", e)
            }
        } catch (e: FeignException) {
            logger.error("Error communicating with $componentName: status=${e.status()}, message=${e.message}")
            throw createException("Communication failed with $componentName", e)
        } catch (e: Exception) {
            logger.error("Unexpected error calling $componentName", e)
            throw createException("Unexpected error integrating with $componentName", e)
        }
    }
}