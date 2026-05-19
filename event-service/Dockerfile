FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY event-service/target/*.jar app.jar
EXPOSE 8094
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
