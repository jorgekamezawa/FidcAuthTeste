plugins {
    application
}

// IMPORTANT: Must be "application" for TAR generation
application {
    mainClass.set("com.banco.fidc.auth.application.BootApplicationKt")
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

    // Feign (required for @EnableFeignClients)
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    // Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    
    // Test Database  
    runtimeOnly("com.h2database:h2")
}