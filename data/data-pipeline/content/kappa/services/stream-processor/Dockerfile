FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY common/target/common-1.0.0.jar /app/lib/
COPY services/stream-processor/target/stream-processor-1.0.0.jar /app/
COPY services/stream-processor/target/dependency/*.jar /app/lib/

ENTRYPOINT ["java", "-cp", "stream-processor-1.0.0.jar:lib/*", "com.teamred.datapipeline.processor.StreamProcessorApplication"]
