# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

COPY pom.xml .
COPY wallet-user-management/pom.xml wallet-user-management/pom.xml

RUN ./mvnw dependency:go-offline -pl wallet-user-management -q

COPY wallet-user-management/src wallet-user-management/src
RUN ./mvnw package -pl wallet-user-management -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/wallet-user-management/target/*.jar app.jar

EXPOSE 8091
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
