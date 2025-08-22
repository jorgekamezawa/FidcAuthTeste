dependencies {
    // Internal modules
    implementation(project(":shared"))
    implementation(project(":domain"))
    implementation(project(":usecase"))

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // JPA & Database
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
}