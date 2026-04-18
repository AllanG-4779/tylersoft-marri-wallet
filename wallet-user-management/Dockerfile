FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY wallet-user-management/target/*.jar app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
