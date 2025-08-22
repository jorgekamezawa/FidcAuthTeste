package com.banco.fidc.auth.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@EnableFeignClients(basePackages = ["com.banco.fidc.auth.external.*"])
@ComponentScan("com.banco.fidc.auth.*")
class BootApplication

fun main(args: Array<String>) {
	runApplication<BootApplication>(*args)
}
