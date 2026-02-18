FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY mock-services/soap-service/target/soap-service-1.0.0-exec.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
