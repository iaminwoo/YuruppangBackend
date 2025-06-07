# 1. 빌드 단계: Gradle + OpenJDK 21
FROM gradle:8.3.1-jdk21 AS build
WORKDIR /app

# 의존성 캐시 위해 build.gradle, settings.gradle 먼저 복사
COPY build.gradle settings.gradle ./
RUN gradle --no-daemon dependencies

# 소스 코드 복사
COPY src ./src

# 빌드 (테스트 제외)
RUN gradle clean build -x test --no-daemon

# 2. 실행 단계: OpenJDK 21
FROM eclipse-temurin:21-jdk
WORKDIR /app

# 빌드 결과물 복사 (jar 위치 맞춰야 함)
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
