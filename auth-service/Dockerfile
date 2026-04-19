FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY auth-service/target/*.jar app.jar
EXPOSE 8093
ENTRYPOINT ["java", "-jar", "app.jar"]
