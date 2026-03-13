package com.electromart;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LineageConsumer {

    private static String lineageTopic;
    private static String timescaleApiUrl;
    private static HttpClient timescaleApiClient;

    public static void main(String[] args) throws Exception {
        String broker = env("KAFKA_BROKER", "kafka:9092");
        lineageTopic = env("TOPIC_LINEAGE", "lineage");
        timescaleApiUrl = env("TIMESCALE_API_URL", "http://timescale-api:8090");
        validateTopicConfig(Map.of("TOPIC_LINEAGE", lineageTopic));
        timescaleApiClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

        waitForKafka(broker);
        waitForTimescaleApi();
        ensureTopic(broker, lineageTopic);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "lineage-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(lineageTopic));

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::close));

        System.out.println("Lineage Consumer started - listening to topic: " + lineageTopic);

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            records.forEach(record -> insertLineage(record.value()));
        }
    }

    private static void insertLineage(String json) {
        try {
            postJson("/api/lineage", json);
        } catch (Exception e) {
            System.err.println("Lineage insert error: " + e.getMessage());
        }
    }

    private static void waitForKafka(String broker) {
        System.out.println("Waiting for Kafka at " + broker + "...");
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", broker))) {
            while (true) {
                try {
                    admin.listTopics().names().get();
                    System.out.println("Kafka is ready");
                    return;
                } catch (Exception e) {
                    sleep(3000);
                }
            }
        }
    }

    private static void waitForTimescaleApi() {
        System.out.println("Waiting for Timescale API...");
        while (true) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(timescaleApiUrl + "/health"))
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

    private static void postJson(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(timescaleApiUrl + path))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = timescaleApiClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Timescale API returned " + response.statusCode() + ": " + response.body());
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
}
