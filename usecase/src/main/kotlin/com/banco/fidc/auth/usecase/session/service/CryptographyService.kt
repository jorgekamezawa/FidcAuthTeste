package com.banco.fidc.auth.usecase.session.service

interface CryptographyService {
    fun generateSecureSessionSecret(): String
}