FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY common/target/common-1.0.0.jar /app/lib/common-1.0.0.jar
COPY services/stream-processor/target/stream-processor-1.0.0.jar app.jar

ENTRYPOINT ["java", "-cp", "app.jar:/app/lib/*", "com.teamred.datapipeline.processor.StreamProcessorApplication"]
