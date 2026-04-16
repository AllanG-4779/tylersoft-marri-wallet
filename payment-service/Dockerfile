# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw

# payment-service depends on the common module — copy both poms
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml

RUN ./mvnw dependency:go-offline -pl payment-service -am -q

COPY common/src common/src
COPY payment-service/src payment-service/src
RUN ./mvnw package -pl payment-service -am -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /workspace/payment-service/target/*.jar app.jar

EXPOSE 8092
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
