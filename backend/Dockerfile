# ============================================================
# Lab 7 - Dockerfile for the BACKEND service (calculator API)
# Two-stage build, same strategy as Lab 6:
#   Stage 1 (build): Maven + JDK compiles the jar
#   Stage 2 (run)  : lightweight JRE image only runs the jar
# Build context: ./backend  (see docker-compose.yml)
# ============================================================

# --- Stage 1: build -----------------------------------------
# FROM: full image with Maven 3.9 and JDK 17, so mvn can run directly.
FROM maven:3.9-eclipse-temurin-17 AS build

# WORKDIR: working directory for every following COPY / RUN.
WORKDIR /app

# COPY: pom.xml first, so the dependency layer stays cached
# as long as the pom does not change.
COPY pom.xml .

# COPY: Maven settings.xml that redirects "central" to the Aliyun mirror,
# because direct access to repo.maven.apache.org is blocked in this network.
COPY settings.xml /root/.m2/settings.xml

# RUN: download all Maven dependencies into this layer (reused cache layer).
# Retry loop: the Docker Desktop proxy in this network resets TLS connections
# intermittently ("Remote host terminated the handshake"), so the command is
# retried up to 40 times; every attempt keeps whatever was already downloaded
# in /root/.m2, so each retry makes progress instead of starting from zero.
RUN i=1; until mvn -B -U dependency:go-offline; do \
        i=$((i+1)); \
        if [ "$i" -gt 40 ]; then echo "dependency download failed 40 times"; exit 1; fi; \
        echo "=== dependency:go-offline failed, retry $i/40 ==="; \
        sleep 5; \
    done

# COPY: the Java sources, including ApiServer.java (HTTP API) and Main.java.
COPY src ./src

# RUN: compile + package. -DskipTests because CI already runs the tests
# before the image build job starts.
# Same retry loop as above (intermittent TLS resets through the proxy).
RUN i=1; until mvn -B -U clean package -DskipTests; do \
        i=$((i+1)); \
        if [ "$i" -gt 40 ]; then echo "mvn package failed 40 times"; exit 1; fi; \
        echo "=== mvn package failed, retry $i/40 ==="; \
        sleep 5; \
    done

# --- Stage 2: runtime ---------------------------------------
# FROM: small JRE-only image, no Maven and no sources shipped.
FROM eclipse-temurin:17-jre-jammy

# WORKDIR: working directory of the final image.
WORKDIR /app

# COPY --from=build: take ONLY the packaged jar out of stage 1.
COPY --from=build /app/target/calculator-1.0.0.jar calculator.jar

# EXPOSE: documents that the API listens on port 8000
# (compose maps host 8000 -> container 8000).
EXPOSE 8000

# PORT / LOG_LEVEL: environment variables read by ApiServer;
# values are injected in docker-compose.yml.
ENV PORT=8000 \
    LOG_LEVEL=INFO

# ENTRYPOINT: fixed command of the container.
ENTRYPOINT ["java", "-jar", "calculator.jar"]

# CMD: default argument "server" starts the HTTP API instead of the CLI,
# so "docker compose up" brings up the API on port 8000.
CMD ["server"]
