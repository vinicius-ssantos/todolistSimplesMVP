# Tickr – Todo List MVP

Tickr is a Spring Boot–based MVP for a simple multi-tenant todo list service. It exposes REST endpoints to register/login users, manage lists and tasks, and enforces JWT-based authentication.

## Tech Stack

- **Runtime:** Kotlin 1.9 + Spring Boot 3.4
- **Persistence:** JPA/Hibernate with Flyway migrations (H2 in-memory by default)
- **Security:** Spring Security with JWT (HS384 by default, RS256 option)
- **Build/Test:** Gradle (Kotlin DSL), JUnit 5, MockK

## Getting Started

### Prerequisites

- Java 21
- Gradle wrapper bundled in the repo

### Environment

Configuration is driven by environment variables (defaults shown in `application.yml`):

| Variable | Purpose | Default |
|----------|---------|---------|
| `SERVER_PORT` | HTTP port | `8081` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | JDBC connection | in-memory H2 |
| `JWT_HS384_SECRET_B64` | Base64 secret for HS384 tokens | Random fallback (64 bytes) |

### Running the app

```bash
./gradlew bootRun
# or specify port/DB etc.
SERVER_PORT=8080 ./gradlew bootRun
```

The API comes up under `/api/**`. Generated Flyway migrations will initialize the schema automatically.

### API documentation

With the server running, Swagger UI is available at `http://localhost:8081/swagger-ui.html`.  
All routes (except `/api/auth/**`) require a Bearer JWT token; the UI includes the `Authorize` button to paste the token returned by `/api/auth/login`.

### Running tests

```bash
./gradlew test
```

A Jacoco report is produced at `build/reports/jacoco/test/html/index.html`.
