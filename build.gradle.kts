plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.2.21"

    id("org.springframework.boot") version "3.4.10"
    id("io.spring.dependency-management") version "1.1.7"

    kotlin("plugin.jpa") version "2.2.21" // ativa no-arg + all-open para JPA
    jacoco
}

group = "com.viniss"
version = "0.0.1-SNAPSHOT"
description = "todolistSimplesMVP"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")


    // Flyway / DB
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("com.h2database:h2:2.2.224")


    // Testes
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.14.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.1")
    testRuntimeOnly("org.postgresql:postgresql")

    // Security + Crypto
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.springframework.security:spring-security-oauth2-jose") // <-- traz nimbus-jose-jwt transitivamente
    implementation("com.nimbusds:nimbus-jose-jwt:10.6")



    // OpenAPI UI (opcional, para Swagger mais adiante)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")


// JWT (jjwt 0.12.x)
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // Rate Limiting (Bucket4j)
    implementation("com.bucket4j:bucket4j-core:8.10.1")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

jacoco {
            toolVersion = "0.8.13"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    environment("DB_URL", System.getenv("DB_URL") ?: "jdbc:h2:mem:todo;DB_CLOSE_DELAY=-1")
    environment("DB_USER", System.getenv("DB_USER") ?: "sa")
    environment("DB_PASSWORD", System.getenv("DB_PASSWORD") ?: "")
    jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

tasks.named<Test>("test") {
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    // Classe principal (application entry point)
                    "**/TodolistSimplesMvpApplicationKt.class",

                    // Classes deprecadas
                    "**/service/TodoListCommandService.class",

                    // Classes não utilizadas / experimental
                    "**/auth/AuthPrincipal.class",
                    "**/auth/NimbusRsaTokenService.class",
                    "**/auth/AuthExceptionHandler.class",

                    // Auto-generated Kotlin data class methods
                    "**/*\$\$*.class",

                    // DTO constructors with default parameters (auto-generated)
                    "**/api/dto/*\$DefaultImpls.class"
                )
            }
        })
    )

    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    // Classe principal (application entry point)
                    "**/TodolistSimplesMvpApplicationKt.class",

                    // Classes deprecadas
                    "**/service/TodoListCommandService.class",

                    // Classes não utilizadas / experimental
                    "**/auth/AuthPrincipal.class",
                    "**/auth/NimbusRsaTokenService.class",
                    "**/auth/AuthExceptionHandler.class",

                    // DTOs and entities (data classes)
                    "**/email/model/*.class",
                    "**/auth/BlacklistedTokenEntity.class",
                    "**/auth/VerifyEmailResponse.class",
                    "**/auth/PasswordValidationResult*.class",
                    "**/auth/RefreshTokenEntity.class",
                    "**/auth/AuthResponse.class",
                    "**/auth/VerifyEmailRequest.class",
                    "**/auth/RefreshTokenRequest.class",

                    // Config classes
                    "**/config/RateLimitConfig.class",

                    // Provider interfaces and implementations not tested
                    "**/email/provider/EmailProvider.class",
                    "**/email/provider/SendGridEmailProvider.class",

                    // Legacy/Deprecated classes
                    "**/auth/AuthService.class",
                    "**/auth/TokenBlacklistService.class",
                    "**/auth/RefreshTokenService.class",

                    // Features avançadas ainda não implementadas
                    "**/domain/RecurrencePattern*.class",
                    "**/domain/SharedListEntity.class",
                    "**/domain/DayOfWeek.class",
                    "**/domain/RecurrenceFrequency.class",
                    "**/domain/TagEntity.class",
                    "**/domain/TaskAttachmentEntity.class",
                    "**/config/HttpsEnforcerConfig.class",

                    // Auto-generated Kotlin data class methods
                    "**/*\$\$*.class",

                    // DTO constructors with default parameters (auto-generated)
                    "**/api/dto/*\$DefaultImpls.class"
                )
            }
        })
    )

    violationRules {
        rule {
            enabled = true
            limit {
                minimum = "0.70".toBigDecimal() // 70% de cobertura mínima
            }
        }
        rule {
            enabled = true
            element = "CLASS"
            limit {
                counter = "LINE"
                minimum = "0.60".toBigDecimal() // 60% por classe
            }

            // Exclui classes específicas das regras de cobertura
            excludes = listOf(
                "com.viniss.todo.TodolistSimplesMvpApplicationKt",
                "com.viniss.todo.service.TodoListCommandService",
                "com.viniss.todo.auth.AuthPrincipal",
                "com.viniss.todo.auth.NimbusRsaTokenService",
                "com.viniss.todo.auth.AuthExceptionHandler",

                // DTOs and entities (data classes)
                "com.viniss.todo.email.model.EmailMessage",
                "com.viniss.todo.auth.BlacklistedTokenEntity",
                "com.viniss.todo.auth.VerifyEmailResponse",
                "com.viniss.todo.auth.PasswordValidationResult*",
                "com.viniss.todo.auth.RefreshTokenEntity",
                "com.viniss.todo.auth.AuthResponse",
                "com.viniss.todo.auth.VerifyEmailRequest",
                "com.viniss.todo.auth.RefreshTokenRequest",

                // Config classes
                "com.viniss.todo.config.RateLimitConfig",

                // Provider interfaces
                "com.viniss.todo.email.provider.EmailProvider",
                "com.viniss.todo.email.provider.SendGridEmailProvider",

                // Legacy/Deprecated classes
                "com.viniss.todo.auth.AuthService",
                "com.viniss.todo.auth.TokenBlacklistService",
                "com.viniss.todo.auth.RefreshTokenService",

                // Features avançadas ainda não implementadas
                "com.viniss.todo.domain.RecurrencePattern*",
                "com.viniss.todo.domain.SharedListEntity",
                "com.viniss.todo.domain.DayOfWeek",
                "com.viniss.todo.domain.RecurrenceFrequency",
                "com.viniss.todo.domain.TagEntity",
                "com.viniss.todo.domain.TaskAttachmentEntity",
                "com.viniss.todo.config.HttpsEnforcerConfig"
            )
        }
        rule {
            enabled = true
            element = "METHOD"
            limit {
                counter = "LINE"
                minimum = "0.50".toBigDecimal() // 50% por método
            }

            // Exclui métodos auto-gerados
            excludes = listOf(
                // Entity getters/setters
                "*.getUserId()",
                "*.setUserId(*)",
                "*.getCreatedAt()",
                "*.setCreatedAt(*)",
                "*.getUpdatedAt()",
                "*.setUpdatedAt(*)",
                "*.getTasks()",
                "*.setTasks(*)",
                "*.getEmail()",

                // equals/hashCode
                "*.equals(*)",
                "*.hashCode()",

                // Kotlin default constructors
                "*.*(*DefaultConstructorMarker)"
            )
        }
    }
}

// Adiciona verificação de cobertura ao check
tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
