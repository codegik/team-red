package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.minio.*;
import io.minio.messages.Item;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class CsvConnector {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final AtomicLong filesProcessed = new AtomicLong(0);
    private static final AtomicLong recordsProcessed = new AtomicLong(0);
    private static final String[] HEADERS = {
        "sale_id", "product_code", "product_name", "category", "brand",
        "salesman_name", "salesman_email", "region", "store_name", "city",
        "store_type", "quantity", "unit_price", "total_amount", "status", "sale_timestamp"
    };
    private static final Set<String> NUMERIC_FIELDS = Set.of("quantity", "unit_price", "total_amount");

    private static KafkaProducer<String, String> producer;
    private static MinioClient minioClient;
    private static String kafkaTopic;
    private static String sourceBucket;
    private static String processedBucket;

    public static void main(String[] args) throws Exception {
        String broker = env("KAFKA_BROKER", "kafka:9092");
        kafkaTopic = env("TOPIC_RAW_SALES", env("KAFKA_TOPIC", "raw-sales"));
        validateTopicConfig(Map.of("TOPIC_RAW_SALES", kafkaTopic));
        String minioEndpoint = env("MINIO_ENDPOINT", "http://minio:9000");
        sourceBucket = env("MINIO_BUCKET", "sales-csv");
        processedBucket = env("MINIO_PROCESSED_BUCKET", "sales-csv-processed");
        int webhookPort = Integer.parseInt(env("WEBHOOK_PORT", "8085"));

        System.out.printf("CSV Connector | Kafka: %s | MinIO: %s | Port: %d%n", broker, minioEndpoint, webhookPort);

        minioClient = MinioClient.builder()
            .endpoint(minioEndpoint)
            .credentials(env("MINIO_ACCESS_KEY", "minioadmin"), env("MINIO_SECRET_KEY", "minioadmin123"))
            .build();

        waitForMinio();
        waitForKafka(broker);
        ensureTopic(broker, kafkaTopic);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        producer = new KafkaProducer<>(props);
        Runtime.getRuntime().addShutdownHook(new Thread(producer::close));

        long pollIntervalSec = Long.parseLong(env("POLL_INTERVAL_SECONDS", "10"));

        processExistingFiles();
        startWebhookServer(webhookPort);
        startPollingFallback(pollIntervalSec);

        System.out.println("Ready - listening for MinIO events + polling every " + pollIntervalSec + "s");
        Thread.currentThread().join();
    }

    private static String env(String key, String defaultValue) {
        return System.getenv().getOrDefault(key, defaultValue);
    }

    private static void startWebhookServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/health", ex -> {
            String resp = String.format("{\"status\":\"healthy\",\"files\":%d,\"records\":%d}", 
                filesProcessed.get(), recordsProcessed.get());
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, resp.length());
            ex.getResponseBody().write(resp.getBytes());
            ex.getResponseBody().close();
        });

        server.createContext("/minio/events", ex -> {
            if ("POST".equals(ex.getRequestMethod())) handleMinioEvent(ex);
            else ex.sendResponseHeaders(405, -1);
        });

        server.createContext("/", ex -> {
            if ("POST".equals(ex.getRequestMethod())) {
                handleMinioEvent(ex);
            } else {
                ex.sendResponseHeaders(200, 0);
                ex.getResponseBody().close();
            }
        });

        server.setExecutor(null);
        server.start();
    }

    private static void handleMinioEvent(HttpExchange ex) throws IOException {
        try {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (!body.isEmpty()) {
                JsonNode records = mapper.readTree(body).get("Records");
                if (records != null && records.isArray()) {
                    for (JsonNode rec : records) {
                        if (rec.path("eventName").asText("").startsWith("s3:ObjectCreated:")) {
                            String bucket = rec.path("s3").path("bucket").path("name").asText();
                            String key = URLDecoder.decode(rec.path("s3").path("object").path("key").asText(), StandardCharsets.UTF_8);
                            if (key.endsWith(".csv") && bucket.equals(sourceBucket)) {
                                processFile(bucket, key);
                            }
                        }
                    }
                }
            }
            ex.sendResponseHeaders(200, 0);
        } catch (Exception e) {
            ex.sendResponseHeaders(500, 0);
        }
        ex.getResponseBody().close();
    }

    private static void processExistingFiles() {
        try {
            for (Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder().bucket(sourceBucket).build())) {
                String key = result.get().objectName();
                if (key.endsWith(".csv")) processFile(sourceBucket, key);
            }
        } catch (Exception ignored) {}
    }

    private static void processFile(String bucket, String key) {
        long start = System.currentTimeMillis();
        try {
            var response = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
            List<String> lines = new BufferedReader(new InputStreamReader(response, StandardCharsets.UTF_8)).lines().toList();
            
            if (lines.size() <= 1) return;

            int count = 0;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String traceId = UUID.randomUUID().toString().substring(0, 8);
                String saleId = line.split(",", 2)[0];
                long recordStart = System.currentTimeMillis();

                String json = csvLineToJson(line, traceId);
                if (json == null) continue;

                producer.send(new ProducerRecord<>(kafkaTopic, saleId, json));
                count++;
            }

            producer.flush();
            moveToProcessed(bucket, key);
            filesProcessed.incrementAndGet();
            recordsProcessed.addAndGet(count);

            System.out.printf("%s -> %s | %d records | %dms%n", key, kafkaTopic, count, System.currentTimeMillis() - start);
        } catch (Exception e) {
            System.err.printf("Error processing %s: %s%n", key, e.getMessage());
        }
    }

    private static void moveToProcessed(String bucket, String key) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                .bucket(processedBucket).object(key)
                .source(CopySource.builder().bucket(bucket).object(key).build())
                .build());
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception ignored) {}
    }

    private static String csvLineToJson(String line, String traceId) {
        try {
            String[] values = line.split(",", -1);
            if (values.length < HEADERS.length) return null;

            ObjectNode node = mapper.createObjectNode();
            node.put("trace_id", traceId);
            node.put("source", "csv");
            node.put("source_version", "v1");

            for (int i = 0; i < HEADERS.length; i++) {
                String field = HEADERS[i], value = values[i].trim();
                if (NUMERIC_FIELDS.contains(field)) {
                    node.put(field, field.equals("quantity") ? Integer.parseInt(value) : Double.parseDouble(value));
                } else {
                    node.put(field, value);
                }
            }
            node.put("ingested_at", Instant.now().toString());
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
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

    private static void startPollingFallback(long intervalSeconds) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "csv-poll-fallback");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int before = (int) recordsProcessed.get();
                processExistingFiles();
                int after = (int) recordsProcessed.get();
                int picked = after - before;
                if (picked > 0) {
                    System.out.printf("[poll-fallback] Picked up %d records from unprocessed files%n", picked);
                }
            } catch (Exception e) {
                System.err.println("Poll fallback error: " + e.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private static void waitForMinio() throws Exception {
        while (true) {
            try {
                minioClient.bucketExists(BucketExistsArgs.builder().bucket(sourceBucket).build());
                return;
            } catch (Exception e) { Thread.sleep(2000); }
        }
    }

    private static void waitForKafka(String broker) throws Exception {
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", broker))) {
            while (true) {
                try { admin.listTopics().names().get(); return; }
                catch (Exception e) { Thread.sleep(2000); }
            }
        }
    }

    private static void ensureTopic(String broker, String topic) {
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", broker))) {
            if (!admin.listTopics().names().get().contains(topic)) {
                admin.createTopics(List.of(new NewTopic(topic, 3, (short) 1))).all().get();
            }
        } catch (Exception ignored) {}
    }
}
