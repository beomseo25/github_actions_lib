# 기존의 AS builder 스테이지(1단계)를 통째로 날려버림!
# 오직 자바 17 실행 환경만 준비합니다.
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 깃허브 액션(yml)이 다운로드해 준 완성품 JAR 파일만 쏙 복사해 옵니다.
COPY build/libs/*.jar ./app.jar

# 웹 터미널 연동을 위한 ttyd 설치 (기존과 동일)
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    ca-certificates \
    && wget https://github.com/tsl0922/ttyd/releases/download/1.7.7/ttyd.x86_64 -O /usr/bin/ttyd \
    && chmod +x /usr/bin/ttyd \
    && apt-get purge -y --auto-remove wget \
    && rm -rf /var/lib/apt/lists/*

# 보안을 위한 실행 유저 설정 (기존과 동일)
RUN useradd -m -u 1001 appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# 전달받은 app.jar 실행
CMD ["ttyd", "-p", "8080", "-W", "java", "-jar", "app.jar"]
