# syntax=docker/dockerfile:1

FROM gradle:8.11-jdk21 AS build
WORKDIR /workspace
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle gradle
COPY src src
RUN chmod +x gradlew
# bootJar sem rodar tests/checks (build Docker mais rápido)
RUN ./gradlew bootJar -x test -x check --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8082
ENTRYPOINT ["java","-jar","app.jar"]
