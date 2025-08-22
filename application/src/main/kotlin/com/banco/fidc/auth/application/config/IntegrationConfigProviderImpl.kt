package com.banco.fidc.auth.application.config

import com.banco.fidc.auth.usecase.session.configprovider.IntegrationConfigProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class IntegrationConfigProviderImpl(
    @Value("\${external-apis.user-management.base-url:http://localhost:8081}")
    private val userManagementBaseUrl: String,
    
    @Value("\${external-apis.fidc-permission.base-url:http://localhost:8082}")
    private val fidcPermissionBaseUrl: String,
    
    @Value("\${feign.client.config.default.connect-timeout:10000}")
    private val timeoutMillis: Int,
    
    @Value("\${feign.client.config.default.retry-attempts:3}")
    private val retryAttempts: Int
) : IntegrationConfigProvider {
    
    override fun getUserManagementBaseUrl(): String = userManagementBaseUrl
    
    override fun getFidcPermissionBaseUrl(): String = fidcPermissionBaseUrl
    
    override fun getTimeoutSeconds(): Int = timeoutMillis / 1000
    
    override fun getRetryAttempts(): Int = retryAttempts
}