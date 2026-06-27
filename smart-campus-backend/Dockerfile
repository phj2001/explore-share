# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build

# 先拷贝依赖描述，利用层缓存加速重复构建
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# 拷贝源码并打包（跳过测试以加速；镜像构建阶段不依赖数据库）
COPY src src
RUN ./mvnw -B -q clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 安装 curl 供容器健康检查使用（修复 #8）
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 非 root 运行
RUN groupadd -r app && useradd -r -g app app

COPY --from=builder /build/target/smart-campus-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
