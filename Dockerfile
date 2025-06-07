# 1. 빌드 단계: Gradle + OpenJDK 21
FROM gradle:8.3-jdk21 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle --no-daemon dependencies

COPY src ./src
RUN gradle clean build -x test --no-daemon

# 2. 실행 단계: OpenJDK 21
FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
