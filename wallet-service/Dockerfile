# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# Copy poms first — changes to source won't bust this layer
COPY pom.xml .
COPY wallet-service/pom.xml wallet-service/pom.xml

# Resolve all external dependencies (cached until poms change)
RUN ./mvnw dependency:go-offline -pl wallet-service -q

# Copy source and build
COPY wallet-service/src wallet-service/src
RUN ./mvnw package -pl wallet-service -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/wallet-service/target/*.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
