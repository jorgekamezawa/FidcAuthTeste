package com.banco.fidc.auth.web.common.extension

import com.banco.fidc.auth.web.common.util.ClientIpResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/**
 * Extension functions para HttpServletRequest
 */

/**
 * Obtém o IP real do cliente considerando headers de proxy/load balancer
 * 
 * @return IP do cliente
 */
fun HttpServletRequest.getClientIp(): String {
    return SpringContextHolder.clientIpResolver.resolveClientIp(this)
}

/**
 * Helper para acessar beans do Spring Context em extension functions
 */
@Component
class SpringContextHolder : ApplicationContextAware {
    
    companion object {
        private lateinit var context: ApplicationContext
        
        val clientIpResolver: ClientIpResolver
            get() = context.getBean(ClientIpResolver::class.java)
    }
    
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        context = applicationContext
    }
}