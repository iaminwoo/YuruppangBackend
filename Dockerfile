# 1. 빌드 단계: OpenJDK 21 + Gradle Wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 권한 설정 (gradlew가 실행 가능하도록)
COPY gradlew .
RUN chmod +x gradlew

# 의존성 캐시를 위해 wrapper, 설정 파일 먼저 복사
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성 다운로드
RUN ./gradlew --no-daemon dependencies

# 전체 소스 복사
COPY src ./src

# 빌드 (테스트 제외)
RUN ./gradlew clean build -x test --no-daemon

# 2. 실행 단계: OpenJDK 21
FROM eclipse-temurin:21-jdk
WORKDIR /app

# 빌드된 JAR만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 스프링 기본 포트
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
