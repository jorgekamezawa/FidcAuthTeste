package com.banco.fidc.auth.repository.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackages = ["com.banco.fidc.auth.repository.jpa"])
@EnableJpaAuditing
@EnableTransactionManagement
class JpaConfig