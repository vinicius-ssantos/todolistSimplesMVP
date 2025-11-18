plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "1.9.25"

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
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
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
        }
        rule {
            enabled = true
            element = "METHOD"
            limit {
                counter = "LINE"
                minimum = "0.50".toBigDecimal() // 50% por método
            }
        }
    }
}

// Adiciona verificação de cobertura ao check
tasks.named("check") {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}
