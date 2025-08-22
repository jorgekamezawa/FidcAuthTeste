package com.banco.fidc.auth.web.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Value("\${spring.application.name}")
    private lateinit var applicationName: String

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("$applicationName API")
                    .description("Sistema de autenticação FIDC - Gestão de sessões de usuário")
                    .version("1.0.0")
            )
            .servers(
                listOf(
                    Server()
                        .url("/")
                        .description("Current server")
                )
            )
    }
}