FROM openjdk:17-jdk-slim AS builder
WORKDIR /build
COPY src/ ./src/
RUN mkdir -p bin && javac -d bin -encoding UTF-8 $(find src -name "*.java")

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /build/bin ./bin

RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    ca-certificates \
    && wget https://github.com/tsl0922/ttyd/releases/download/1.7.7/ttyd.x86_64 -O /usr/bin/ttyd \
    && chmod +x /usr/bin/ttyd \
    && apt-get purge -y --auto-remove wget \
    && rm -rf /var/lib/apt/lists/*

RUN useradd -m -u 1001 appuser && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

CMD ["ttyd", "-p", "8080", "-W", "java", "-cp", "bin", "LibraryMain"]
