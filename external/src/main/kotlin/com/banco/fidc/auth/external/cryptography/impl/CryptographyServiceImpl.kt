package com.banco.fidc.auth.external.cryptography.impl

import com.banco.fidc.auth.usecase.session.service.CryptographyService
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom

@Service
class CryptographyServiceImpl : CryptographyService {
    
    companion object {
        private const val SECRET_ENTROPY_BYTES = 32 // 256 bits de entropia para SHA-256
    }

    override fun generateSecureSessionSecret(): String {
        val random = SecureRandom()
        val randomBytes = ByteArray(SECRET_ENTROPY_BYTES)
        random.nextBytes(randomBytes)
        
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(randomBytes)
        
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}