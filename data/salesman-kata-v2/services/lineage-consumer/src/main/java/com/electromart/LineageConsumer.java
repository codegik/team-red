package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LineageConsumer {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String LINEAGE_TOPIC = "lineage";

    private static Connection dbConnection;
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    private static final String INSERT_SQL = """
        INSERT INTO lineage (trace_id, sale_id, stage, component, event_type, 
            source_topic, target_topic, latency_ms, metadata, event_timestamp)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
        """;

    public static void main(String[] args) throws Exception {
        String broker = env("KAFKA_BROKER", "kafka:9092");
        dbUrl = env("TIMESCALEDB_URL", "jdbc:postgresql://timescaledb:5432/salesdb");
        dbUser = env("TIMESCALEDB_USER", "sales");
        dbPassword = env("TIMESCALEDB_PASSWORD", "sales123");

        waitForTimescaleDB();
        ensureTopic(broker, LINEAGE_TOPIC);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "lineage-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(LINEAGE_TOPIC));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumer.close();
            closeDb();
        }));

        System.out.println("Lineage Consumer started - listening to topic: " + LINEAGE_TOPIC);

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            records.forEach(record -> insertLineage(record.value()));
        }
    }

    private static void insertLineage(String json) {
        try {
            JsonNode node = mapper.readTree(json);

            Connection conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
                ps.setString(1, node.path("trace_id").asText());
                ps.setString(2, node.path("sale_id").asText(null));
                ps.setString(3, node.path("stage").asText());
                ps.setString(4, node.path("component").asText());
                ps.setString(5, node.path("event_type").asText());
                ps.setString(6, node.path("source_topic").asText(null));
                ps.setString(7, node.path("target_topic").asText(null));
                ps.setLong(8, node.path("latency_ms").asLong(0));
                ps.setString(9, node.has("metadata") ? node.get("metadata").toString() : null);

                String timestamp = node.path("timestamp").asText();
                if (timestamp != null && !timestamp.isEmpty()) {
                    ps.setTimestamp(10, Timestamp.from(Instant.parse(timestamp)));
                } else {
                    ps.setTimestamp(10, Timestamp.from(Instant.now()));
                }

                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("Lineage insert error: " + e.getMessage());
            closeDb();
        }
    }

    private static Connection getConnection() throws SQLException {
        if (dbConnection == null || dbConnection.isClosed()) {
            dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        }
        return dbConnection;
    }

    private static void closeDb() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) dbConnection.close();
        } catch (Exception ignored) {}
        dbConnection = null;
    }

    private static void waitForTimescaleDB() {
        while (true) {
            try {
                dbConnection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                System.out.println("TimescaleDB is ready");
                return;
            } catch (Exception e) {
                sleep(3000);
            }
        }
    }

    private static void ensureTopic(String broker, String topic) {
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", broker))) {
            if (!admin.listTopics().names().get().contains(topic)) {
                admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
            }
        } catch (Exception ignored) {}
    }

    private static String env(String key, String def) {
        return System.getenv().getOrDefault(key, def);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
