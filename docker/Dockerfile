FROM gradle:8.14.3-jdk21-alpine AS build
WORKDIR /app
COPY . .
RUN gradle clean :core:core-api:bootJar --no-daemon

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /app/core/core-api/build/libs/core-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx1024m", "-jar", "app.jar"]
