package com.banco.fidc.auth.external.config.aws

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region

@Configuration
class AwsBaseConfig(
    @Value("\${aws.region:us-east-1}") private val awsRegion: String,
    @Value("\${aws.local-stack.enable:false}") private val localStack: Boolean
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Bean
    fun awsCredentialsProvider(): AwsCredentialsProvider {
        logger.info("Configuring AWS credentials provider")
        return DefaultCredentialsProvider.create()
    }

    @Bean
    fun awsRegion(): Region {
        logger.info("AWS Region configured: $awsRegion")
        return Region.of(awsRegion)
    }

    @Bean
    fun isAwsLocalStack(): Boolean = localStack
}