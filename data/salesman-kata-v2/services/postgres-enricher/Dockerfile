FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ ./src/
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /app/target/postgres-enricher-1.0.0.jar app.jar

ENV JAVA_OPTS="-Xms32M -Xmx64M"
CMD java $JAVA_OPTS -jar app.jar
