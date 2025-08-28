dependencies {
    // Internal modules
    implementation(project(":shared"))
    api(project(":usecase")) // API porque implementa interfaces do usecase

    // Spring Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Feign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // AWS SDK
    implementation("software.amazon.awssdk:secretsmanager")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.3")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // Redis for session storage  
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
}