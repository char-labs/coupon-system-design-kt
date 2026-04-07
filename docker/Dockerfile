# Build stage
FROM gradle:9.1.0-jdk25-alpine AS build
WORKDIR /app

ARG APP_MODULE=:coupon:coupon-api
ARG APP_JAR_DIR=coupon/coupon-api

# Copy gradle files for dependency caching
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Copy all module build files
COPY coupon/coupon-enum/build.gradle.kts coupon/coupon-enum/
COPY coupon/coupon-domain/build.gradle.kts coupon/coupon-domain/
COPY coupon/coupon-api/build.gradle.kts coupon/coupon-api/
COPY coupon/coupon-worker/build.gradle.kts coupon/coupon-worker/
COPY storage/db-core/build.gradle.kts storage/db-core/
COPY storage/redis/build.gradle.kts storage/redis/
COPY support/logging/build.gradle.kts support/logging/
COPY support/monitoring/build.gradle.kts support/monitoring/

# Download dependencies (for caching)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY coupon coupon
COPY storage storage
COPY support support

# Build application
RUN gradle clean ${APP_MODULE}:bootJar --no-daemon -x test

# Runtime stage
FROM amazoncorretto:25-alpine
WORKDIR /app

ARG APP_JAR_DIR=coupon/coupon-api

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/${APP_JAR_DIR}/build/libs/*.jar app.jar

# Spring Boot 4 & Java 25 optimizations for Amazon Corretto
ENV JAVA_OPTS="-Xmx1024m -Xms512m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.backgroundpreinitializer.ignore=true"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
