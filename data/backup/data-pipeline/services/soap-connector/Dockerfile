FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY common/target/common-1.0.0.jar /app/lib/
COPY services/soap-connector/target/soap-connector-1.0.0.jar /app/
COPY services/soap-connector/target/dependency/*.jar /app/lib/

ENTRYPOINT ["java", "-cp", "soap-connector-1.0.0.jar:lib/*", "com.teamred.datapipeline.soapconnector.SoapConnectorApplication"]
