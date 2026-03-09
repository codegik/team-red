package com.electromart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class PostgresConnector {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public static void main(String[] args) throws Exception {
        String connectUrl = env("KAFKA_CONNECT_URL", "http://kafka-connect:8083");
        String connectorName = env("CONNECTOR_NAME", "postgres-connector");

        System.out.printf("Postgres Connector | Kafka Connect: %s%n", connectUrl);

        waitForKafkaConnect(connectUrl);
        registerConnector(connectUrl, connectorName);

        System.out.println("Done - connector registered");
    }

    private static String env(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }

    private static void waitForKafkaConnect(String connectUrl) throws Exception {
        while (true) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(connectUrl + "/connectors"))
                    .GET()
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    System.out.println("Kafka Connect is ready");
                    return;
                }
            } catch (Exception ignored) {}
            Thread.sleep(2000);
        }
    }

    private static void registerConnector(String connectUrl, String connectorName) throws Exception {
        ObjectNode config = mapper.createObjectNode();
        config.put("name", connectorName);

        ObjectNode connectorConfig = config.putObject("config");
        connectorConfig.put("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
        connectorConfig.put("database.hostname", env("DB_HOST", "postgres"));
        connectorConfig.put("database.port", env("DB_PORT", "5432"));
        connectorConfig.put("database.user", env("DB_USER", "electromart"));
        connectorConfig.put("database.password", env("DB_PASSWORD", "electromart123"));
        connectorConfig.put("database.dbname", env("DB_NAME", "electromart"));
        connectorConfig.put("topic.prefix", "electromart");
        connectorConfig.put("table.include.list", "public.sales,public.products,public.salesmen,public.stores");
        connectorConfig.put("plugin.name", "pgoutput");
        connectorConfig.put("slot.name", "sales_slot");
        connectorConfig.put("publication.name", "sales_publication");
        connectorConfig.put("publication.autocreate.mode", "filtered");
        connectorConfig.put("snapshot.mode", "initial");
        connectorConfig.put("decimal.handling.mode", "double");
        connectorConfig.put("time.precision.mode", "connect");
        connectorConfig.put("tombstones.on.delete", "false");
        connectorConfig.put("transforms", "unwrap");
        connectorConfig.put("transforms.unwrap.type", "io.debezium.transforms.ExtractNewRecordState");
        connectorConfig.put("transforms.unwrap.drop.tombstones", "true");
        connectorConfig.put("transforms.unwrap.delete.handling.mode", "drop");
        connectorConfig.put("key.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("key.converter.schemas.enable", "false");
        connectorConfig.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        connectorConfig.put("value.converter.schemas.enable", "false");

        String body = mapper.writeValueAsString(config);

        HttpRequest getRequest = HttpRequest.newBuilder()
            .uri(URI.create(connectUrl + "/connectors/" + connectorName))
            .GET()
            .build();
        HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());

        if (getResponse.statusCode() == 200) {
            HttpRequest putRequest = HttpRequest.newBuilder()
                .uri(URI.create(connectUrl + "/connectors/" + connectorName + "/config"))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(connectorConfig)))
                .build();
            client.send(putRequest, HttpResponse.BodyHandlers.ofString());
        } else {
            HttpRequest postRequest = HttpRequest.newBuilder()
                .uri(URI.create(connectUrl + "/connectors"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            client.send(postRequest, HttpResponse.BodyHandlers.ofString());
        }

        Thread.sleep(1000);
        HttpRequest statusRequest = HttpRequest.newBuilder()
            .uri(URI.create(connectUrl + "/connectors/" + connectorName + "/status"))
            .GET()
            .build();
        HttpResponse<String> statusResponse = client.send(statusRequest, HttpResponse.BodyHandlers.ofString());
        System.out.printf("Connector status: %s%n", statusResponse.body());
    }
}
