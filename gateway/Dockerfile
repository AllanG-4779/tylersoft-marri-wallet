FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
