# 🚀 INITIAL-SETUP - CONFIGURAÇÃO INICIAL DO PROJETO

---
id: initial-setup
version: 1.0.0
requires: [meta-prompt, project-context]
provides: [gradle-structure, docker-setup, spring-config, helm-charts]
optional: false
---

## 🎯 SEU PAPEL

Você irá gerar toda a estrutura inicial do projeto baseado no contexto definido anteriormente. Isso inclui configuração Gradle multimodule, Docker com TAR para deployment, configurações Spring e Helm charts.

## 📋 PRÉ-REQUISITOS

Antes de iniciar, verifique:
1. **Contexto do projeto disponível** - nome, group, stack definida
2. **Tecnologias identificadas** - para gerar apenas configurações necessárias
3. **Fluxos mapeados** - para entender integrações necessárias

## 🔄 FLUXO DE GERAÇÃO

### 1. Confirmação do Artifact ID e Package
```
"Vi que o projeto se chama [nome]. 

Para a estrutura Maven/Gradle, sugiro:
- Group ID: com.empresa
- Artifact ID: [nome-kebab-case]
- Package base: com.empresa.[nome.em.pontos]

Exemplo:
- Se o nome for 'fidc-password':
  - Artifact ID: fidc-password ✓
  - Package: com.empresa.fidc.password ✓

⚠️ **IMPORTANTE**: Se o nome tiver hífen (-), no package Java/Kotlin 
precisamos trocar por ponto (.). 

No seu caso:
- Artifact ID: [nome-com-hifen]
- Package: com.empresa.[nome.com.pontos]

Está correto ou prefere ajustar?
(Aceito variações no group como com.btg.pactual)"
```

### 2. Coleta de Informações Específicas

**SE PostgreSQL foi identificado:**
```
"Para o PostgreSQL local, preciso das credenciais:
- Nome do banco: [sugestão: nome_underscore_db]
- Usuário: [sugestão: nome_underscore_user]
- Senha: [sua escolha]

Confirma essas configurações?"
```

**SE AWS (SQS/S3/Secrets) foi identificado:**
```
"Para os recursos AWS locais (LocalStack):

Filas SQS identificadas:
- [nomeFila1]: [propósito]
- [nomeFila2]: [propósito]

Buckets S3:
- [bucket-name]: [tipo de arquivos]

Secrets Manager:
- ldap-config: Configurações do LDAP
- jwt-secret: Chave para tokens JWT
- [outros-secrets]: [propósito]

Confirma esses nomes?"
```

**SE Redis foi identificado:**
```
"Para o Redis, usaremos:
- Host: localhost
- Porta: 6379
- Sem senha (local)
- Database: 0
- Pool Lettuce configurado

Precisa ajustar algo?"
```

**Para Helm Charts:**
```
"Para os Helm charts, qual é o nome da squad responsável?
Isso será usado na configuração de deployment."
```

## 📁 ESTRUTURA A SER GERADA

### 1. GRADLE - Estrutura Base

#### settings.gradle.kts
```kotlin
rootProject.name = "[artifact-id]"
include("application")
include("domain")
include("external")
include("repository")
include("shared")
include("usecase")
include("web")
```

#### build.gradle.kts (root)
```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    [SE USAR JPA]
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.4.7"
    id("io.spring.dependency-management") version "1.1.7"
}

allprojects {
    group = "[group-id]"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    [SE USAR JPA]
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    [SE USAR SPRING CLOUD]
    extra["springCloudVersion"] = "2024.0.2"
    
    dependencyManagement {
        imports {
            [SE USAR SPRING CLOUD]
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
            [SE USAR AWS SDK]
            mavenBom("software.amazon.awssdk:bom:2.20.26")
        }
    }

    dependencies {
        // Kotlin
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        
        // Test
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // Disable bootJar for all submodules except 'application'
    if (project.name != "application") {
        tasks.bootJar {
            enabled = false
        }
    }
}

// Disable bootJar on root project
tasks.bootJar {
    enabled = false
}
```

#### build.gradle.kts por módulo

**shared/build.gradle.kts**
```kotlin
// Shared module - Pure Kotlin, no external dependencies
```

**domain/build.gradle.kts**
```kotlin
dependencies {
    // Internal modules
    implementation(project(":shared"))
}
```

**usecase/build.gradle.kts**
```kotlin
dependencies {
    // Internal modules
    implementation(project(":shared"))
    implementation(project(":domain"))
    
    // Spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-tx")
}
```

**repository/build.gradle.kts**
```kotlin
dependencies {
    // Internal modules
    implementation(project(":shared"))
    implementation(project(":domain"))
    implementation(project(":usecase"))
    
    [SE TIVER REDIS]
    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    [SE TIVER JPA]
    // JPA & Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
}
```

**external/build.gradle.kts**
```kotlin
dependencies {
    // Internal modules
    implementation(project(":shared"))
    implementation(project(":domain"))
    implementation(project(":usecase"))
    
    // Spring Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    [SE TIVER FEIGN]
    // Feign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    
    [SE TIVER AWS]
    // AWS SDK
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("software.amazon.awssdk:sqs")
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:s3")
    
    [SE TIVER SQS LISTENER]
    // Spring Cloud AWS
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs:3.0.0")
    
    [SE TIVER JWT]
    // JWT
    implementation("com.auth0:java-jwt:4.4.0")
    
    [SE TIVER LDAP]
    // LDAP
    implementation("org.springframework.ldap:spring-ldap-core:3.1.2")
    implementation("org.springframework.boot:spring-boot-starter-data-ldap")
}
```

**web/build.gradle.kts**
```kotlin
dependencies {
    // Internal modules
    implementation(project(":shared"))
    implementation(project(":usecase"))
    
    // Spring Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
}
```

**application/build.gradle.kts**
```kotlin
plugins {
    application
}

// IMPORTANT: Must be "application" for TAR generation
application {
    mainClass.set("[package].application.BootApplicationKt")
    applicationName = "application"
}

dependencies {
    // All internal modules
    implementation(project(":shared"))
    implementation(project(":domain"))
    implementation(project(":usecase"))
    implementation(project(":repository"))
    implementation(project(":web"))
    implementation(project(":external"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    [SE TIVER FEIGN]
    // Feign (required for @EnableFeignClients)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    
    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")
    
    [SE TIVER CACHE]
    // Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    
    [SE PRECISAR CONFIGURAR REDIS/LDAP/AWS]
    // Additional dependencies for configurations and health checks
    [adicionar conforme necessário]
}
```

### 2. DOCKER

#### Dockerfile
```dockerfile
# TODO: Replace with corporate JRE image when available
FROM amazoncorretto:21-alpine3.18-jre

RUN apk update && apk add --no-cache curl
VOLUME /tmp
RUN mkdir /app
ADD application/build/distributions/application-0.0.1-SNAPSHOT.tar /app/

# TODO: Add Datadog configurations when provided by the company

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "/app/application-0.0.1-SNAPSHOT/bin/application"]
```

#### docker-compose.yml
```yaml
version: '3.8'

services:
  [APENAS SERVIÇOS NECESSÁRIOS BASEADOS NO CONTEXTO]
  
  [SE TIVER POSTGRESQL]
  postgres:
    image: postgres:16-alpine
    container_name: [nome-projeto]-postgres
    environment:
      POSTGRES_DB: [nome_confirmado]
      POSTGRES_USER: [usuario_confirmado]
      POSTGRES_PASSWORD: [senha_confirmada]
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - dev-network

  [SE TIVER REDIS]
  redis:
    image: redis:7-alpine
    container_name: [nome-projeto]-redis
    ports:
      - "6379:6379"
    networks:
      - dev-network

  [SE TIVER AWS (SQS/S3/SECRETS)]
  localstack:
    image: localstack/localstack:3.0
    container_name: [nome-projeto]-localstack
    environment:
      SERVICES: [apenas necessários: sqs,s3,secretsmanager,sns]
      AWS_DEFAULT_REGION: us-east-1
      EDGE_PORT: 4566
    ports:
      - "4566:4566"
    volumes:
      - "./docker/localstack/init-aws.sh:/etc/localstack/init/ready.d/init-aws.sh"
    networks:
      - dev-network

volumes:
  [volumes necessários]

networks:
  dev-network:
    driver: bridge
```

[SE TIVER AWS - docker/localstack/init-aws.sh]
```bash
#!/bin/bash

echo "Initializing LocalStack resources..."

[SE TIVER S3]
# Create S3 buckets
awslocal s3 mb s3://[bucket-name]

[SE TIVER SQS]
# Create SQS queues
awslocal sqs create-queue --queue-name [nomeFila]

[SE TIVER SNS]
# Create SNS topics
awslocal sns create-topic --name [topic-name]

[SE TIVER SECRETS MANAGER]
# Create secrets
awslocal secretsmanager create-secret \
  --name dev/ldap/config \
  --secret-string '{
    "ldapServerSsl": "ldaps://ldap.company.com:636",
    "base": "DC=company,DC=com",
    "userDn": "CN={0},OU=ServiceAccounts",
    "username": "svc-account",
    "password": "localpass"
  }'

awslocal secretsmanager create-secret \
  --name dev/jwt/secret \
  --secret-string '{
    "signingKey": "local-secret-key-for-development-only"
  }'

echo "LocalStack initialized successfully!"
```

### 3. SPRING CONFIGURATION

Os arquivos de configuração a seguir devem ser criados no módulo **application**:

#### application/src/main/resources/application.yml
```yaml
spring:
  application:
    name: [artifact-id]
  
  profiles:
    active: local
  
  lifecycle:
    timeout-per-shutdown-phase: 120s
  
  [SE TIVER REDIS]
  data:
    redis:
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
  
  jackson:
    deserialization:
      fail-on-unknown-properties: false
    time-zone: America/Sao_Paulo

server:
  port: 8080
  shutdown: graceful
  servlet:
    context-path: /[nome-kebab-case]
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
  error:
    whitelabel:
      enabled: false

management:
  endpoints:
    web:
      base-path: /public/actuator
      exposure:
        include: health,info,metrics,prometheus
      cors:
        allowed-origins: "*"
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

# OpenAPI/Swagger
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui/index.html
    try-it-out-enabled: true

logging:
  level:
    root: INFO
    [package]: INFO
    org.springframework.web: WARN

[SE TIVER CONFIGURAÇÕES CUSTOMIZADAS]
properties:
  [contexto]:
    [config]: [valor]
```

#### application/src/main/resources/application-local.yml
```yaml
[SE TIVER REDIS]
spring:
  data:
    redis:
      host: localhost
      port: 6379

[SE TIVER POSTGRESQL]
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/[nome_db]
    username: [usuario]
    password: [senha]
    driver-class-name: org.postgresql.Driver
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true

[SE TIVER AWS]
aws:
  region: us-east-1
  local-stack:
    enable: true
    endpoint: http://localhost:4566
  [SE TIVER SECRETS]
  secret-manager:
    ldap:
      name: dev/ldap/config
    jwt:
      name: dev/jwt/secret

[SE TIVER FEIGN/APIS EXTERNAS]
external-apis:
  [nome-servico]:
    base-url: http://localhost:[porta]

[SE TIVER FEIGN]
feign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 10000
        logger-level: full

# Custom properties - hardcoded values
properties:
  [contexto]:
    [propriedade]: [valor-fixo]

logging:
  level:
    root: INFO
    [package]: DEBUG
    org.springframework.web: DEBUG
    [SE TIVER FEIGN]
    org.springframework.cloud.openfeign: DEBUG
```

#### application/src/main/resources/application-dev.yml
```yaml
[MESMAS SEÇÕES DO LOCAL MAS COM VARIÁVEIS E DEFAULTS]

# Example:
properties:
  token-mfa:
    cooldown-seconds: ${TOKEN_MFA_COOLDOWN:30}
    
external-apis:
  user-management:
    base-url: ${USER_MANAGEMENT_URL:http://user-management-dev.internal:8080}
```

#### application/src/main/resources/application-uat.yml
```yaml
[IDÊNTICO AO ARQUIVO application-prd.yml ABAIXO]
```

#### application/src/main/resources/application-prd.yml
```yaml
[APENAS VARIÁVEIS SEM DEFAULTS]

# Example:
properties:
  token-mfa:
    cooldown-seconds: ${TOKEN_MFA_COOLDOWN}
    
external-apis:
  user-management:
    base-url: ${USER_MANAGEMENT_URL}
```

#### application/src/main/resources/logback-spring.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_PATTERN_LOCAL" 
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %highlight(%-5level) [%thread] [%X{correlationId:-}] %cyan(%logger{36}) - %msg%n%xException{10}"/>

    <springProfile name="local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>${LOG_PATTERN_LOCAL}</pattern>
            </encoder>
        </appender>

        <root level="${logging.level.root:-INFO}">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="!local">
        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp>
                        <timeZone>America/Sao_Paulo</timeZone>
                    </timestamp>
                    <logLevel/>
                    <threadName/>
                    <mdc/>
                    <loggerName>
                        <shortenedLoggerNameLength>36</shortenedLoggerNameLength>
                    </loggerName>
                    <message/>
                    <stackTrace>
                        <throwableConverter class="net.logstash.logback.stacktrace.ShorteningThrowableConverter">
                            <maxDepthPerThrowable>30</maxDepthPerThrowable>
                            <shortenedClassNameLength>36</shortenedClassNameLength>
                            <rootCauseFirst>true</rootCauseFirst>
                        </throwableConverter>
                    </stackTrace>
                    <pattern>
                        <pattern>
                            {
                            "application": "${SPRING_APPLICATION_NAME:-[artifact-id]}",
                            "environment": "${SPRING_PROFILES_ACTIVE:-default}",
                            "version": "${project.version:-0.0.1-SNAPSHOT}"
                            }
                        </pattern>
                    </pattern>
                </providers>
            </encoder>
        </appender>

        <appender name="CONSOLE_ASYNC" class="ch.qos.logback.classic.AsyncAppender">
            <queueSize>512</queueSize>
            <discardingThreshold>0</discardingThreshold>
            <appender-ref ref="CONSOLE"/>
        </appender>

        <root level="${logging.level.root:-INFO}">
            <appender-ref ref="CONSOLE_ASYNC"/>
        </root>
    </springProfile>
</configuration>
```

### 4. CLASSES BASE

#### application/src/main/kotlin/[package/path]/application/BootApplication.kt
```kotlin
package [package].application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
[SE TIVER FEIGN]
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
[SE TIVER FEIGN]
@EnableFeignClients(basePackages = ["[package].external.*"])
@ComponentScan("[package].*")
class BootApplication

fun main(args: Array<String>) {
    runApplication<BootApplication>(*args)
}
```

#### application/src/main/kotlin/[package/path]/application/config/TimeZoneConfig.kt
```kotlin
package [package].application.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import java.util.TimeZone

@Configuration
class TimeZoneConfig {

    companion object {
        private val log = LoggerFactory.getLogger(TimeZoneConfig::class.java)
        private const val BRAZIL_TIMEZONE = "America/Sao_Paulo"
    }

    @PostConstruct
    fun init() {
        val timezone = TimeZone.getTimeZone(BRAZIL_TIMEZONE)
        TimeZone.setDefault(timezone)
        
        check(TimeZone.getDefault().id == BRAZIL_TIMEZONE) {
            "Failed to configure timezone. Expected: $BRAZIL_TIMEZONE, Current: ${TimeZone.getDefault().id}"
        }
        
        log.info("Timezone configured to: $BRAZIL_TIMEZONE (${timezone.displayName})")
    }
}
```

### 5. CLASSES CONDICIONAIS

[GERAR APENAS SE TIVER CACHE]
#### application/src/main/kotlin/[package/path]/application/config/CacheConfig.kt
```kotlin
package [package].application.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val caffeineCacheManager = CaffeineCacheManager()

        caffeineCacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
        )

        // Caches identified in flows
        caffeineCacheManager.setCacheNames(listOf(
            [SE TIVER SECRETS] "secrets",
            [OUTROS CACHES IDENTIFICADOS]
        ))

        return caffeineCacheManager
    }
}
```

[GERAR APENAS SE TIVER REDIS]
#### repository/src/main/kotlin/[package/path]/repository/config/RedisHealthIndicator.kt
```kotlin
package [package].repository.config

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.stereotype.Component

@Component
class RedisHealthIndicator(
    private val redisConnectionFactory: RedisConnectionFactory
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val connection = redisConnectionFactory.connection
            connection.ping()
            connection.close()
            Health.up()
                .withDetail("type", "Redis")
                .build()
        } catch (ex: Exception) {
            Health.down()
                .withDetail("type", "Redis")
                .withException(ex)
                .build()
        }
    }
}
```

[GERAR SE TIVER PROPRIEDADES CUSTOMIZADAS]
#### application/src/main/kotlin/[package/path]/application/config/properties/
```kotlin
// For each group of properties identified
package [package].application.config.properties

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

@Validated
@Configuration
@ConfigurationProperties(prefix = "properties.[contexto]")
data class [Contexto]Properties(
    @field:NotBlank(message = "[campo] cannot be empty")
    var [campo]: String = "",
    
    @field:Min(value = 1, message = "[campo] must be greater than 0")
    var [campo]: Int = 0
)

// Provider implementation in application module
package [package].application.config.provider

import [package].application.config.properties.[Contexto]Properties
import [package].usecase.[contexto].configprovider.[Contexto]ConfigProvider
import org.springframework.stereotype.Service

@Service
class [Contexto]ConfigProviderImpl(
    private val properties: [Contexto]Properties
) : [Contexto]ConfigProvider {
    override fun get[Campo](): String = properties.[campo]
}
```

[SEMPRE GERAR]
#### web/src/main/kotlin/[package/path]/web/filter/CorrelationIdFilter.kt
```kotlin
package [package].web.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : OncePerRequestFilter() {

    companion object {
        const val CORRELATION_ID_HEADER = "x-correlation-id"
        const val CORRELATION_ID_MDC_KEY = "correlationId"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val correlationId = request.getHeader(CORRELATION_ID_HEADER) 
            ?: UUID.randomUUID().toString()

        MDC.put(CORRELATION_ID_MDC_KEY, correlationId)
        response.setHeader(CORRELATION_ID_HEADER, correlationId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(CORRELATION_ID_MDC_KEY)
        }
    }
}
```

### 6. HELM CHARTS

#### helm/values-dev.yml
```yaml
application:
  name: [nome-kebab-case]
  squad: [NOME DA SQUAD INFORMADO]
  version: $VERSION
  base_deploy_image: 21-alpine3.18-jre
  base_build_image: 21-alpine3.18-jdk

infra:
  env:
    name: dev
    javaOpts: -Denv.type=dev -XX:+UseG1GC -XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=75.0

  deploy:
    maxSurge: 50%
    maxUnavailable: 0
    terminationGracePeriodSeconds: 135

    resources:
      requests:
        cpu: 200m
        memory: 512Mi
      limits:
        memory: 768Mi

    healthcheck:
      path: /public/actuator/health

      livenessProbe:
        initialDelaySeconds: 60
        periodSeconds: 10
        timeoutSeconds: 10
        failureThreshold: 5

      readinessProbe:
        initialDelaySeconds: 30
        periodSeconds: 10
        timeoutSeconds: 10
        failureThreshold: 3

  autoscaling:
    enabled: true
    minReplicas: 1
    maxReplicas: 2
    targetCPUUtilizationPercentage: 60

  ingress:
    path: /[nome-kebab-case]
    class: nginx-internal

  armArch: true

  dockerfile:
    custom_steps: ''
    custom_entrypoint: 'application/build/distributions/application-0.0.1-SNAPSHOT.tar'

observability:
  enabled: false
```

#### helm/values-uat.yml
```yaml
application:
  name: [nome-kebab-case]
  squad: [NOME DA SQUAD INFORMADO]
  version: $VERSION
  base_deploy_image: 21-alpine3.18-jre
  base_build_image: 21-alpine3.18-jdk

infra:
  env:
    name: uat
    javaOpts: -Denv.type=uat -XX:+UseG1GC -XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=75.0

  deploy:
    maxSurge: 50%
    maxUnavailable: 0
    terminationGracePeriodSeconds: 135

    resources:
      requests:
        cpu: 400m
        memory: 1Gi
      limits:
        memory: 1.5Gi

    healthcheck:
      path: /public/actuator/health

      livenessProbe:
        initialDelaySeconds: 60
        periodSeconds: 10
        timeoutSeconds: 10
        failureThreshold: 5

      readinessProbe:
        initialDelaySeconds: 30
        periodSeconds: 10
        timeoutSeconds: 10
        failureThreshold: 3

  autoscaling:
    enabled: true
    minReplicas: 2
    maxReplicas: 4
    targetCPUUtilizationPercentage: 70

  ingress:
    path: /[nome-kebab-case]
    class: nginx-internal

  armArch: true

  dockerfile:
    custom_steps: ''
    custom_entrypoint: 'application/build/distributions/application-0.0.1-SNAPSHOT.tar'

observability:
  enabled: false
```

#### helm/values-prd.yml
```yaml
application:
  name: [nome-kebab-case]
  squad: [NOME DA SQUAD INFORMADO]
  version: $VERSION
  base_deploy_image: 21-alpine3.18-jre
  base_build_image: 21-alpine3.18-jdk

infra:
  env:
    name: prd
    javaOpts: -Denv.type=prd -XX:+UseG1GC -XX:MinRAMPercentage=50.0 -XX:MaxRAMPercentage=75.0

  deploy:
    maxSurge: 50%
    maxUnavailable: 0
    terminationGracePeriodSeconds: 135

    resources:
      requests:
        cpu: 500m
        memory: 2Gi
      limits:
        memory: 3Gi

    healthcheck:
      path: /public/actuator/health

      livenessProbe:
        initialDelaySeconds: 60
        periodSeconds: 10
        timeoutSeconds: 10
        failureThreshold: 5

      readinessProbe:
        initialDelaySeconds: 30
        periodSeconds: 10
        timeoutSeconds: 10
        failureThreshold: 3

  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70

  ingress:
    path: /[nome-kebab-case]
    class: nginx-internal

  armArch: true

  dockerfile:
    custom_steps: ''
    custom_entrypoint: 'application/build/distributions/application-0.0.1-SNAPSHOT.tar'

observability:
  enabled: true
```

## 🎯 PROCESSO DE GERAÇÃO

1. **Confirme artifact ID e package** alertando sobre conversão de hífen para ponto
2. **Pergunte sobre Squad** para Helm charts
3. **Gere em ordem**:
   - Gradle files (settings → root → módulos)
   - Docker files (incluindo init-aws.sh se necessário)
   - Spring configurations (indicando que vão no módulo application)
   - Classes base obrigatórias
   - Classes condicionais baseadas na stack
   - Helm charts
4. **Use artifacts separados** para cada grupo de arquivos
5. **Mantenha versões específicas** conforme definido

## ⚠️ PONTOS DE ATENÇÃO

### Nomenclatura
- **Artifact ID**: Pode ter hífen (fidc-password)
- **Package**: Deve usar ponto (com.empresa.fidc.password)
- **Classe Boot**: Sempre BootApplication independente do nome

### Estrutura
- **Arquivos YML**: Todos no módulo application
- **CorrelationIdFilter**: No módulo web
- **RedisHealthIndicator**: No módulo repository (se usar Redis)
- **Feign dependency**: Necessária no application para @EnableFeignClients

### Deployment
- **application.applicationName**: DEVE ser "application" para gerar TAR correto
- **bootJar**: Desabilitado em subprojects com `if` e no root explicitamente
- **Dockerfile**: Usa TAR diretamente, não JAR

### Configurações
- **Local**: Valores hardcoded
- **Dev**: Variáveis com defaults
- **UAT/PRD**: Apenas variáveis sem defaults (arquivos idênticos)
- **Comentários**: Em inglês nos YMLs para evitar encoding issues

### AWS LocalStack
- Script simples apenas para criar recursos
- Sem mensagens elaboradas ou testes complexos
- Criar recursos específicos identificados nos fluxos

### Health Checks
- Apenas Redis health se usar Redis
- Sem health checks customizados para APIs/integrações

---

### FEEDBACK
<!-- Registro de melhorias durante uso -->

### NOTAS DE VERSÃO
- v1.2.0: Ajustes de nomenclatura (artifact/package), BootApplication padrão, melhor organização de módulos