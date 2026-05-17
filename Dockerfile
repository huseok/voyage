# syntax=docker/dockerfile:1
# 完整容器内编译（CI / 无本机 JDK 时用）。本地日常请用 Dockerfile.fast + 本机 bootJar，见 scripts/docker-local-up.ps1。
FROM gradle:8.14.4-jdk17 AS builder
WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src/ src/
RUN chmod +x gradlew

RUN --mount=type=cache,target=/root/.gradle \
    gradle bootJar -x test --no-daemon --stacktrace --console=plain

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
