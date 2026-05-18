# 1단계: 빌드 스테이지
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build

# 프로젝트 전체 복사
COPY . .

# Gradle을 사용해 실행 가능한 Fat JAR 빌드
RUN chmod +x gradlew && ./gradlew build -x test

# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 빌드 스테이지에서 생성된 JAR 파일을 app.jar라는 이름으로 복사
COPY --from=builder /build/build/libs/*.jar ./app.jar

# 웹 터미널 연동을 위한 ttyd 설치 프로세스 (apt-get 정상 작동)
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    ca-certificates \
    && wget https://github.com/tsl0922/ttyd/releases/download/1.7.7/ttyd.x86_64 -O /usr/bin/ttyd \
    && chmod +x /usr/bin/ttyd \
    && apt-get purge -y --auto-remove wget \
    && rm -rf /var/lib/apt/lists/*

# 보안을 위한 실행 유저 설정
RUN useradd -m -u 1001 appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# JAR 파일로 바로 ttyd 구동
CMD ["ttyd", "-p", "8080", "-W", "java", "-jar", "app.jar"]
