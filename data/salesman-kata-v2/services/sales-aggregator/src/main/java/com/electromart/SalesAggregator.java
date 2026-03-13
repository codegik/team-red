package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class SalesAggregator {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static String INPUT_TOPIC;
    private static String OUTPUT_TOPIC;
    private static String DLQ_TOPIC;
    private static String TIMESCALE_API_URL;

    private static final Set<String> REQUIRED_FIELDS = Set.of(
        "sale_id", "source", "sale_timestamp", "total_amount"
    );

    private static HttpClient timescaleApiClient;

    public static void main(String[] args) throws Exception {
        String broker = env("KAFKA_BROKER", "kafka:9092");
        INPUT_TOPIC = env("TOPIC_RAW_SALES", env("INPUT_TOPIC", "raw-sales"));
        OUTPUT_TOPIC = env("TOPIC_SALES", "sales");
        DLQ_TOPIC = env("TOPIC_DLQ", "sales-dlq");
        TIMESCALE_API_URL = env("TIMESCALE_API_URL", "http://timescale-api:8090");
        validateTopicConfig(Map.of(
            "TOPIC_RAW_SALES", INPUT_TOPIC,
            "TOPIC_SALES", OUTPUT_TOPIC,
            "TOPIC_DLQ", DLQ_TOPIC
        ));
        timescaleApiClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "sales-aggregator");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);

        waitForTopics(broker);
        waitForTimescaleApi();
        ensureTopic(broker, DLQ_TOPIC);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> merged = builder
            .<String, String>stream(INPUT_TOPIC)
            .filter((key, value) -> value != null);

        Map<String, KStream<String, String>> branches = merged
            .split(Named.as("branch-"))
            .branch((key, value) -> validate(value), Branched.as("valid"))
            .defaultBranch(Branched.as("invalid"));

        KStream<String, String> valid = branches.get("branch-valid");
        KStream<String, String> invalid = branches.get("branch-invalid");

        valid.to(OUTPUT_TOPIC);
        valid.foreach((key, value) -> insertSale(value));

        invalid
            .peek((key, value) -> System.err.printf(
                "[DLQ] Schema violation - key=%s missing required fields. Routing to %s%n", key, DLQ_TOPIC))
            .to(DLQ_TOPIC);

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        System.out.println("Starting Sales Aggregator | input: " + INPUT_TOPIC + " | valid -> " + OUTPUT_TOPIC + " | invalid -> " + DLQ_TOPIC);
        streams.start();
    }

    static boolean validate(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            for (String field : REQUIRED_FIELDS) {
                JsonNode val = node.get(field);
                if (val == null || val.isNull() || val.asText().isBlank()) {
                    System.err.printf("[Validation] Missing or empty required field '%s'%n", field);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static synchronized void insertSale(String json) {
        try {
            postJson("/api/sales", json);
        } catch (Exception e) {
            System.err.println("Insert error: " + e.getMessage());
        }
    }

    private static void waitForTimescaleApi() {
        while (true) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TIMESCALE_API_URL + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
                HttpResponse<String> response = timescaleApiClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    System.out.println("Timescale API is ready");
                    return;
                }
            } catch (Exception ignored) {
            }
            sleep(3000);
        }
    }

    private static JsonNode postJson(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TIMESCALE_API_URL + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = timescaleApiClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Timescale API returned " + response.statusCode() + ": " + response.body());
        }
        if (response.body() == null || response.body().isBlank()) {
            return mapper.createObjectNode();
        }
        return mapper.readTree(response.body());
    }

    private static void waitForTopics(String broker) throws Exception {
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", broker))) {
            while (true) {
                Set<String> existing = admin.listTopics().names().get();
                if (existing.contains(INPUT_TOPIC)) {
                    System.out.println("Input topic '" + INPUT_TOPIC + "' found");
                    return;
                }
                System.out.println("Waiting for topic '" + INPUT_TOPIC + "'...");
                sleep(5000);
            }
        }
    }

    private static String env(String key, String def) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? def : value;
    }

    private static void validateTopicConfig(Map<String, String> topics) {
        List<String> errors = new ArrayList<>();
        for (var entry : topics.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                errors.add(entry.getKey() + " is blank");
            } else if (!entry.getValue().matches("[a-zA-Z0-9._-]+")) {
                errors.add(entry.getKey() + "='" + entry.getValue() + "' contains invalid characters");
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid topic configuration: " + String.join(", ", errors));
        }
        System.out.println("Topic config validated: " + topics);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void ensureTopic(String broker, String topic) {
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", broker))) {
            if (!admin.listTopics().names().get().contains(topic)) {
                admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
            }
        } catch (Exception ignored) {
        }
    }
}
