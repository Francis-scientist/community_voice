import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.communityvoice"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // REST controllers + embedded Tomcat web server.
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Talks to MongoDB Atlas.
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    // Powers the @field:NotBlank style checks on incoming request bodies.
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Just the password hashing part of Spring Security (BCrypt). We do NOT
    // pull in the whole security starter - this project resolves the logged-in
    // user itself, in security/AuthTokenFilter.kt, which is easier to follow.
    implementation("org.springframework.security:spring-security-crypto")

    // Signs and verifies the JSON Web Tokens handed out at login.
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // Lets Jackson build Kotlin data classes from JSON.
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // Makes Kotlin honour Spring's @Nullable / @NonNull annotations strictly.
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}
