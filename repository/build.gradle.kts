dependencies {
    // Internal modules
    implementation(project(":shared"))
    implementation(project(":domain"))
    implementation(project(":external"))

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}