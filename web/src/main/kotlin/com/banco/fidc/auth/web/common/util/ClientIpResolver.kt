package com.banco.fidc.auth.web.common.util

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class ClientIpResolver(
    @Value("\${app.security.allow-localhost}")
    private val allowLocalhost: Boolean
) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    companion object {
        private val IP_HEADERS = listOf(
            "X-Forwarded-For",
            "X-Real-IP", 
            "X-Client-IP",
            "CF-Connecting-IP",
            "True-Client-IP",
            "X-Cluster-Client-IP"
        )
        
        private const val UNKNOWN = "unknown"
        private const val LOCALHOST_IPV4 = "127.0.0.1"
        private const val LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1"
    }
    
    /**
     * Resolve o IP real do cliente a partir do HttpServletRequest
     * Verifica headers de proxy/load balancer na ordem de prioridade
     * 
     * @throws SecurityException se IP não pode ser determinado e localhost não é permitido
     */
    fun resolveClientIp(request: HttpServletRequest): String {
        logger.debug("Resolvendo IP do cliente... (allowLocalhost=$allowLocalhost)")
        
        // Verificar headers de proxy na ordem de prioridade
        for (header in IP_HEADERS) {
            val ip = extractIpFromHeader(request, header)
            if (ip != null) {
                logger.debug("IP do cliente obtido do header '$header': $ip")
                return validateAndReturnIp(ip)
            }
        }
        
        // Fallback para IP direto da conexão
        val remoteAddr = request.remoteAddr
        
        return when {
            remoteAddr.isNullOrBlank() -> handleMissingIp()
            isLocalhostIp(remoteAddr) -> handleLocalhostIp(remoteAddr)
            else -> {
                logger.debug("IP do cliente obtido do remoteAddr: $remoteAddr")
                remoteAddr
            }
        }
    }
    
    /**
     * Extrai IP de um header específico, lidando com casos especiais
     */
    private fun extractIpFromHeader(request: HttpServletRequest, headerName: String): String? {
        val headerValue = request.getHeader(headerName)
        
        if (headerValue.isNullOrBlank() || headerValue.equals(UNKNOWN, ignoreCase = true)) {
            return null
        }
        
        // X-Forwarded-For pode conter múltiplos IPs: "client, proxy1, proxy2"
        val ip = if (headerName == "X-Forwarded-For") {
            headerValue.split(",").firstOrNull()?.trim()
        } else {
            headerValue.trim()
        }
        
        return if (isValidIp(ip)) ip else null
    }
    
    /**
     * Valida se o IP é válido e não é um placeholder
     */
    private fun isValidIp(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false
        if (ip.equals(UNKNOWN, ignoreCase = true)) return false
        
        // Verificação básica de formato IPv4
        if (isValidIPv4(ip)) return true
        
        // Verificação básica de formato IPv6
        if (isValidIPv6(ip)) return true
        
        return false
    }
    
    /**
     * Validação simples de IPv4
     */
    private fun isValidIPv4(ip: String): Boolean {
        val parts = ip.split(".")
        if (parts.size != 4) return false
        
        return parts.all { part ->
            try {
                val num = part.toInt()
                num in 0..255
            } catch (e: NumberFormatException) {
                false
            }
        }
    }
    
    /**
     * Validação simples de IPv6
     */
    private fun isValidIPv6(ip: String): Boolean {
        // Validação básica - aceita formatos como ::1, 2001:db8::1, etc.
        val ipv6Regex = Regex("^([0-9a-fA-F]{1,4}:){1,7}[0-9a-fA-F]{0,4}$|^::1$|^::$")
        return ipv6Regex.matches(ip)
    }
    
    /**
     * Valida e retorna IP, verificando se é localhost quando necessário
     */
    private fun validateAndReturnIp(ip: String): String {
        return if (isLocalhostIp(ip)) {
            handleLocalhostIp(ip)
        } else {
            ip
        }
    }
    
    /**
     * Verifica se o IP é localhost (IPv4 ou IPv6)
     */
    private fun isLocalhostIp(ip: String): Boolean {
        return ip == LOCALHOST_IPV4 || 
               ip == "127.0.0.1" || 
               ip == LOCALHOST_IPV6 || 
               ip == "::1" ||
               ip.startsWith("127.")
    }
    
    /**
     * Trata caso onde IP não pode ser determinado
     */
    private fun handleMissingIp(): String {
        return if (allowLocalhost) {
            logger.warn("IP não encontrado - usando localhost (modo desenvolvimento)")
            LOCALHOST_IPV4
        } else {
            logger.error("IP do cliente não pode ser determinado em ambiente de produção")
            throw SecurityException("IP do cliente não pode ser determinado")
        }
    }
    
    /**
     * Trata caso onde IP é localhost
     */
    private fun handleLocalhostIp(ip: String): String {
        return if (allowLocalhost) {
            logger.debug("IP localhost aceito (modo desenvolvimento): $ip")
            ip
        } else {
            logger.error("Tentativa de acesso via localhost rejeitada em ambiente de produção: $ip")
            throw SecurityException("Acesso localhost não permitido em produção")
        }
    }
}