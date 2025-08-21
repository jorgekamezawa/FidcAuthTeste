dependencies {
    // Internal modules
    implementation(project(":shared"))
    implementation(project(":usecase"))

    // Spring Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // OpenAPI Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
}