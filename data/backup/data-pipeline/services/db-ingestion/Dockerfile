FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY common/target/common-1.0.0.jar /app/lib/
COPY services/db-ingestion/target/db-ingestion-1.0.0.jar /app/
COPY services/db-ingestion/target/dependency/*.jar /app/lib/

ENTRYPOINT ["java", "-cp", "db-ingestion-1.0.0.jar:lib/*", "com.teamred.datapipeline.dbingestion.DbIngestionApplication"]
