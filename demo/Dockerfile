FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 1단계: 의존성 관련 파일만 먼저 복사
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

# 2단계: 의존성만 먼저 다운로드 (소스 안 바꾸면 이 레이어 캐시됨)
RUN ./gradlew dependencies --no-daemon

# 3단계: 소스 복사 + 빌드 (소스 바꾸면 여기서부터만 다시 실행)
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]