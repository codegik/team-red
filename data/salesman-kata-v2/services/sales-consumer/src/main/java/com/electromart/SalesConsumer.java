package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class SalesConsumer {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final AtomicLong duplicatesSkipped = new AtomicLong(0);
    private static final AtomicLong timestampFallbacks = new AtomicLong(0);
    private static LongHistogram pipelineLatency;
    private static LongCounter duplicatesCounter;
    private static LongCounter timestampFallbackCounter;

    private static final String INSERT_SQL = """
        INSERT INTO sales (
            sale_id, source, product_code, product_name, category, brand,
            salesman_name, salesman_email, region, store_name, city, store_type,
            quantity, unit_price, total_amount, status, sale_timestamp, trace_id
        ) VALUES (
            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
        ) ON CONFLICT (sale_id, sale_timestamp) DO NOTHING
        """;

    public static void main(String[] args) {
        String broker = env("KAFKA_BROKER", "kafka:9092");
        String topic = env("KAFKA_TOPIC_SALES", "sales");
        String groupId = env("KAFKA_GROUP_ID", "timescale-sales-writer");

        String dbHost = env("TIMESCALEDB_HOST", "timescaledb");
        String dbPort = env("TIMESCALEDB_PORT", "5432");
        String dbUser = env("TIMESCALEDB_USER", "sales");
        String dbPassword = env("TIMESCALEDB_PASSWORD", "sales123");
        String dbName = env("TIMESCALEDB_DATABASE", "salesdb");
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);

        Meter meter = GlobalOpenTelemetry.getMeter("sales-consumer");
        pipelineLatency = meter
            .histogramBuilder("pipeline.processing.duration")
            .setDescription("End-to-end latency from connector pickup to TimescaleDB insert")
            .setUnit("ms")
            .ofLongs()
            .build();
        duplicatesCounter = meter
            .counterBuilder("pipeline.duplicate.inserts")
            .setDescription("Records skipped due to ON CONFLICT (duplicate sale_id + sale_timestamp)")
            .build();
        timestampFallbackCounter = meter
            .counterBuilder("pipeline.timestamp.fallbacks")
            .setDescription("Records where sale_timestamp failed to parse and fell back to current time")
            .build();

        startHealthServer(8086);

        Connection conn = waitForDatabase(jdbcUrl, dbUser, dbPassword);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down consumer...");
            consumer.wakeup();
        }));

        consumer.subscribe(List.of(topic));
        System.out.printf("Sales Consumer started | topic: %s | group: %s | db: %s%n", topic, groupId, jdbcUrl);

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    if (record.value() == null) continue;

                    try {
                        insertSale(conn, record.value());
                    } catch (SQLException e) {
                        if (!conn.isValid(5)) {
                            System.err.println("Connection lost, reconnecting...");
                            conn = waitForDatabase(jdbcUrl, dbUser, dbPassword);
                        }
                        try {
                            insertSale(conn, record.value());
                        } catch (Exception retryEx) {
                            System.err.printf("Failed to insert sale from partition %d offset %d: %s%n",
                                record.partition(), record.offset(), retryEx.getMessage());
                        }
                    }
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            System.err.println("Consumer error: " + e.getMessage());
        } finally {
            consumer.close();
            try { conn.close(); } catch (Exception ignored) {}
            System.out.println("Consumer stopped");
        }
    }

    private static void insertSale(Connection conn, String json) throws Exception {
        JsonNode node = mapper.readTree(json);

        try (PreparedStatement stmt = conn.prepareStatement(INSERT_SQL)) {
            stmt.setString(1, node.path("sale_id").asText());
            stmt.setString(2, node.path("source").asText());
            stmt.setString(3, textOrNull(node, "product_code"));
            stmt.setString(4, textOrNull(node, "product_name"));
            stmt.setString(5, textOrNull(node, "category"));
            stmt.setString(6, textOrNull(node, "brand"));
            stmt.setString(7, textOrNull(node, "salesman_name"));
            stmt.setString(8, textOrNull(node, "salesman_email"));
            stmt.setString(9, textOrNull(node, "region"));
            stmt.setString(10, textOrNull(node, "store_name"));
            stmt.setString(11, textOrNull(node, "city"));
            stmt.setString(12, textOrNull(node, "store_type"));
            stmt.setInt(13, node.path("quantity").asInt(0));
            stmt.setDouble(14, node.path("unit_price").asDouble(0));
            stmt.setDouble(15, node.path("total_amount").asDouble(0));
            stmt.setString(16, textOrNull(node, "status"));
            stmt.setTimestamp(17, parseTimestamp(node.path("sale_timestamp").asText()));
            stmt.setString(18, textOrNull(node, "trace_id"));

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.printf("Inserted sale %s%n", node.path("sale_id").asText());
                String pickedUpAt = node.path("picked_up_at").asText(null);
                if (pickedUpAt != null && !pickedUpAt.isBlank()) {
                    try {
                        long latencyMs = Instant.now().toEpochMilli() - Instant.parse(pickedUpAt).toEpochMilli();
                        pipelineLatency.record(latencyMs, Attributes.of(
                            AttributeKey.stringKey("source"), node.path("source").asText("unknown")
                        ));
                    } catch (Exception ignored) {}
                }
            } else {
                duplicatesSkipped.incrementAndGet();
                duplicatesCounter.add(1);
            }
        }
    }

    private static Timestamp parseTimestamp(String value) {
        try {
            return Timestamp.from(Instant.parse(value));
        } catch (Exception e) {
            timestampFallbacks.incrementAndGet();
            timestampFallbackCounter.add(1);
            return Timestamp.from(Instant.now());
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode val = node.get(field);
        if (val == null || val.isNull() || val.asText().isBlank()) return null;
        return val.asText();
    }

    private static Connection waitForDatabase(String jdbcUrl, String user, String password) {
        while (true) {
            try {
                Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
                conn.setAutoCommit(true);
                System.out.println("Connected to TimescaleDB");
                return conn;
            } catch (SQLException e) {
                System.out.printf("Waiting for TimescaleDB at %s ...%n", jdbcUrl);
                sleep(3000);
            }
        }
    }

    private static void startHealthServer(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/health", ex -> {
                String resp = String.format(
                    "{\"status\":\"healthy\",\"duplicates_skipped\":%d,\"timestamp_fallbacks\":%d}",
                    duplicatesSkipped.get(), timestampFallbacks.get());
                ex.getResponseHeaders().add("Content-Type", "application/json");
                ex.sendResponseHeaders(200, resp.length());
                ex.getResponseBody().write(resp.getBytes());
                ex.getResponseBody().close();
            });
            server.start();
            System.out.printf("Health endpoint started on port %d%n", port);
        } catch (Exception e) {
            System.err.printf("Failed to start health server: %s%n", e.getMessage());
        }
    }

    private static String env(String key, String def) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? def : value;
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
