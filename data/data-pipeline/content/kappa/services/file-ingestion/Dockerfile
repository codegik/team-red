FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY common/target/common-1.0.0.jar /app/lib/
COPY services/file-ingestion/target/file-ingestion-1.0.0.jar /app/
COPY services/file-ingestion/target/dependency/*.jar /app/lib/

ENTRYPOINT ["java", "-cp", "file-ingestion-1.0.0.jar:lib/*", "com.teamred.datapipeline.fileingestion.FileIngestionApplication"]
