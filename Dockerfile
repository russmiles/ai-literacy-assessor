# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies separately so Docker can cache this layer. If only
# source files change (not pom.xml), the dependency layer is reused, cutting
# rebuild time from ~2 min to ~30s in typical development cycles.
# Note: -B (batch) suppresses interactive prompts; SNAPSHOT repos may produce
# warnings about missing checksums — these are harmless and expected.
RUN mvn -B dependency:go-offline -q || true
COPY src/ src/
RUN mvn -B package -DskipTests

# Runtime stage — Eclipse Temurin JRE on Alpine for a minimal footprint.
# Alpine was chosen over Debian slim because the JRE is the dominant size
# contributor; Alpine's smaller base saves ~30 MB and reduces the attack surface.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/alci-assessor-*.jar /app/alci-assessor.jar

# /repo is mounted by the caller when a local repository scan is desired.
# /assessments is where the agent writes the output markdown report.
# Both are optional — the agent runs without either (conversational-only mode).
RUN mkdir -p /repo /assessments
VOLUME ["/repo", "/assessments"]

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/alci-assessor.jar"]
