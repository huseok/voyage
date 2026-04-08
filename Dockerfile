# 多阶段构建：容器内完成编译与打包，不依赖本地 build/libs
FROM gradle:8.10.2-jdk17 AS builder
WORKDIR /workspace
COPY . .
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
