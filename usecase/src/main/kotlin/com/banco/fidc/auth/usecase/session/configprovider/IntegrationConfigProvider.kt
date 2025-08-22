package com.banco.fidc.auth.usecase.session.configprovider

interface IntegrationConfigProvider {
    fun getUserManagementBaseUrl(): String
    fun getFidcPermissionBaseUrl(): String
    fun getTimeoutSeconds(): Int
    fun getRetryAttempts(): Int
}