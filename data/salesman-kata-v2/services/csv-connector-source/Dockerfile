FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ ./src/
RUN mvn package -DskipTests

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /app/target/csv-connector-1.0.0.jar app.jar

ENV JAVA_OPTS="-Xms32M -Xmx64M"
CMD java $JAVA_OPTS -jar app.jar
