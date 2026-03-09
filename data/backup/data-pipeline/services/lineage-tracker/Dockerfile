FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY common/target/common-1.0.0.jar /app/lib/
COPY services/lineage-tracker/target/lineage-tracker-1.0.0.jar /app/
COPY services/lineage-tracker/target/dependency/*.jar /app/lib/

ENTRYPOINT ["java", "-cp", "lineage-tracker-1.0.0.jar:lib/*", "com.teamred.datapipeline.lineage.LineageTrackerApplication"]
