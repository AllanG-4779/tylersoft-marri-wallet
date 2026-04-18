FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY payment-service/target/*.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
