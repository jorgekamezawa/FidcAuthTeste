dependencies {
    // Internal modules
    implementation(project(":shared"))
    implementation(project(":domain"))

    // Spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-tx")
}