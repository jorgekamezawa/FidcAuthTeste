plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.springframework.boot") version "3.4.8"
	id("io.spring.dependency-management") version "1.1.7"
}

allprojects {
	group = "com.banco.fidc.auth"
	version = "0.0.1-SNAPSHOT"

	repositories {
		mavenCentral()
	}
}

subprojects {
	apply(plugin = "org.jetbrains.kotlin.jvm")
	apply(plugin = "org.jetbrains.kotlin.plugin.spring")
	apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
	apply(plugin = "org.springframework.boot")
	apply(plugin = "io.spring.dependency-management")

	java {
		toolchain {
			languageVersion = JavaLanguageVersion.of(21)
		}
	}

	extra["springCloudVersion"] = "2024.0.2"

	dependencyManagement {
		imports {
			mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
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
