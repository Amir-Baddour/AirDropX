# ---------- Stage 1: build the shaded JAR with Maven ----------
FROM public.ecr.aws/docker/library/maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src

# Download dependencies first so this layer is cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

# Build (no source code changes: this is the project exactly as it is)
COPY src ./src
RUN mvn -B -q -DskipTests package \
 && find target -maxdepth 1 -name "*.jar" ! -name "original-*" -exec cp {} /app.jar \;

# ---------- Stage 2: small runtime image ----------
FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --uid 10001 --home /app appuser

COPY --from=build /app.jar /app/app.jar

# The app reads /app/.env (Config uses Dotenv.load() from the working dir).
# The .env is mounted at runtime by docker compose. It is NEVER baked into the image.
USER appuser
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fsS http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
