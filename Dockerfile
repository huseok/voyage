# syntax=docker/dockerfile:1
# 本地 docker compose / Docker Desktop：与原先一致，仅多 COPY gradle.properties（Kotlin 进程内编译，减轻部分云构建问题）。
# Kaniko 等若仍报 /root/.kotlin 快照错误：用 Dockerfile.fast（本机先 bootJar）或 CI 先产出 JAR 再构建。见 DEPLOY.md §6.2.1、§6.3。
FROM gradle:8.14.4-jdk17 AS builder
WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties ./
RUN chmod +x gradlew

COPY src/ src/

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew bootJar -x test --no-daemon --stacktrace

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
