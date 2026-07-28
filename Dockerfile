# ===== 1단계: 빌드 (Gradle wrapper 가 Gradle 을 내려받아 bootJar 생성) =====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 의존성 레이어 캐시: 빌드 스크립트만 먼저 복사해 의존성 해석을 캐시한다
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
# Windows 체크아웃 대비 CRLF 제거 + 실행권한
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew --no-daemon dependencies --quiet || true

COPY src ./src
# 테스트는 CI/로컬에서 통과 확인 후 이미지를 만드는 전제 — 이미지 빌드에서는 생략
RUN ./gradlew --no-daemon bootJar -x test

# ===== 2단계: 실행 (JRE 만 포함한 경량 이미지) =====
FROM eclipse-temurin:17-jre
WORKDIR /app

# 비루트 사용자로 실행
RUN useradd --system --create-home lxp \
    && mkdir -p /data/uploads/content \
    && chown -R lxp:lxp /data/uploads
USER lxp

COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

# 업로드 파일(VOD·문서·이수증 등)은 볼륨으로 영속화
VOLUME /data/uploads
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms512m -Xmx1024m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
