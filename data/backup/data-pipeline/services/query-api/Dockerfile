FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY services/query-api/target/query-api-1.0.0-exec.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]
