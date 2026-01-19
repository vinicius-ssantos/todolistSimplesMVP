# syntax=docker/dockerfile:1

FROM gradle:9.3-jdk21 AS build
WORKDIR /workspace

# Copiar apenas arquivos de configuração do Gradle primeiro
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle gradle

# Baixar dependências separadamente (essa camada será cacheada)
RUN chmod +x gradlew && \
    ./gradlew dependencies --no-daemon || true

# Agora copiar o código fonte
COPY src src

# Fazer o build (muito mais rápido se as dependências estiverem cacheadas)
RUN ./gradlew bootJar -x test -x check --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8082
ENTRYPOINT ["java","-jar","app.jar"]
