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
    implementation("com.auth0:java-jwt:4.4.0")

    // LDAP
    implementation("org.springframework.ldap:spring-ldap-core:3.1.2")
    implementation("org.springframework.boot:spring-boot-starter-data-ldap")
}