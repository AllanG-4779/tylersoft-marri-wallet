FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY wallet-service/target/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
