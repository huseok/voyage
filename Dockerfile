# syntax=docker/dockerfile:1
# 使用 wrapper 与项目一致；BuildKit 缓存 ~/.gradle，重复构建会快很多。
# 若仍觉得慢：本机先 `.\gradlew.bat bootJar -x test`，再用 Dockerfile.fast（见 DEPLOY.md）。
FROM gradle:8.14.4-jdk17 AS builder
WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew

# 先拷依赖相关文件，再拷源码，改业务代码时可命中缓存
COPY src/ src/

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew bootJar -x test --no-daemon --stacktrace

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
