package com.banco.fidc.auth.usecase.session.configprovider

interface SessionConfigProvider {
    fun getTtlMinutes(): Int
    fun getCleanupIntervalMinutes(): Int
}