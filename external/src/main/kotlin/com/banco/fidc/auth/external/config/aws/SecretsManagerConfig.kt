package com.banco.fidc.auth.external.config.aws

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.net.URI

@Configuration
class SecretsManagerConfig(
    private val awsCredentialsProvider: AwsCredentialsProvider,
    private val awsRegion: Region,
    private val isAwsLocalStack: Boolean,
    @Value("\${aws.local-stack.endpoint:http://localhost:4566}") private val localEndpoint: String
) {
    
    @Bean
    fun secretsManagerClient(): SecretsManagerClient {
        val builder = SecretsManagerClient.builder()
            .region(awsRegion)
            .credentialsProvider(awsCredentialsProvider)
        
        if (isAwsLocalStack) {
            builder.endpointOverride(URI.create(localEndpoint))
        }
        
        return builder.build()
    }
}