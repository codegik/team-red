package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.ArrayList;

public class SalesAggregator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String COMPONENT_NAME = "sales-aggregator";

    private static String INPUT_TOPIC;
    private static String OUTPUT_TOPIC;
    private static String DLQ_TOPIC;
    private static String LINEAGE_TOPIC;

    // Canonical SaleEvent required fields — any record missing these goes to the DLQ
    private static final Set<String> REQUIRED_FIELDS = Set.of(
        "sale_id", "source", "sale_timestamp", "total_amount"
    );

    private static volatile Connection dbConnection;
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    private static KafkaProducer<String, String> lineageProducer;
    private static boolean lineageEnabled;

    private static final String INSERT_SQL = """
        INSERT INTO sales (sale_id, source, product_code, product_name, category, brand,
            salesman_name, salesman_email, region, store_name, city, store_type,
            quantity, unit_price, total_amount, status, sale_timestamp, trace_id)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (sale_id, sale_timestamp) DO NOTHING
        """;

    public static void main(String[] args) throws Exception {
        String broker = env("KAFKA_BROKER", "kafka:9092");
        INPUT_TOPIC = env("TOPIC_RAW_SALES", env("INPUT_TOPIC", "raw-sales"));
        OUTPUT_TOPIC = env("TOPIC_SALES", "sales");
        DLQ_TOPIC = env("TOPIC_DLQ", "sales-dlq");
        LINEAGE_TOPIC = env("TOPIC_LINEAGE", "lineage");
        validateTopicConfig(Map.of(
            "TOPIC_RAW_SALES", INPUT_TOPIC, "TOPIC_SALES", OUTPUT_TOPIC,
            "TOPIC_DLQ", DLQ_TOPIC, "TOPIC_LINEAGE", LINEAGE_TOPIC
        ));
        dbUrl = env("TIMESCALEDB_URL", "jdbc:postgresql://timescaledb:5432/salesdb");
        dbUser = env("TIMESCALEDB_USER", "sales");
        dbPassword = env("TIMESCALEDB_PASSWORD", "sales123");
        lineageEnabled = Boolean.parseBoolean(env("LINEAGE_ENABLED", "true"));

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "sales-aggregator");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);

        waitForTopics(broker);
        waitForTimescaleDB();
        ensureTopic(broker, DLQ_TOPIC);

        if (lineageEnabled) {
            ensureTopic(broker, LINEAGE_TOPIC);
            Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            lineageProducer = new KafkaProducer<>(producerProps);
            System.out.println("Lineage tracking: ENABLED");
        }

        StreamsBuilder builder = new StreamsBuilder();

        // Connectors now own normalization — aggregator only validates the canonical contract
        KStream<String, String> merged = builder
            .<String, String>stream(INPUT_TOPIC)
            .filter((key, value) -> value != null);

        Map<String, KStream<String, String>> branches = merged
            .split(Named.as("branch-"))
            .branch((key, value) -> validate(value), Branched.as("valid"))
            .defaultBranch(Branched.as("invalid"));

        KStream<String, String> valid   = branches.get("branch-valid");
        KStream<String, String> invalid = branches.get("branch-invalid");

        valid.to(OUTPUT_TOPIC);
        valid.foreach((key, value) -> insertSale(value));

        invalid
            .peek((key, value) -> System.err.printf(
                "[DLQ] Schema violation — key=%s missing required fields. Routing to %s%n", key, DLQ_TOPIC))
            .to(DLQ_TOPIC);

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            streams.close();
            if (lineageProducer != null) lineageProducer.close();
            closeDb();
        }));

        System.out.println("Starting Sales Aggregator | input: " + INPUT_TOPIC + " | valid -> " + OUTPUT_TOPIC + " | invalid -> " + DLQ_TOPIC);
        streams.start();
    }

    /**
     * Validates that a record conforms to the canonical SaleEvent schema.
     * All format normalization is now the responsibility of each source connector.
     * Records that fail validation are routed to the DLQ topic.
     */
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
            JsonNode node = mapper.readTree(json);
            String traceId = node.path("trace_id").asText();
            String saleId = node.get("sale_id").asText();
            String source = node.path("source").asText("unknown");

            Connection conn = getConnection();
            try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
                ps.setString(1, saleId);
                ps.setString(2, source);
                ps.setString(3, node.path("product_code").asText());
                ps.setString(4, node.path("product_name").asText());
                ps.setString(5, node.path("category").asText());
                ps.setString(6, node.path("brand").asText());
                ps.setString(7, node.path("salesman_name").asText());
                ps.setString(8, node.path("salesman_email").asText());
                ps.setString(9, node.path("region").asText());
                ps.setString(10, node.path("store_name").asText());
                ps.setString(11, node.path("city").asText());
                ps.setString(12, node.path("store_type").asText());
                ps.setInt(13, node.path("quantity").asInt(0));
                ps.setDouble(14, node.path("unit_price").asDouble(0));
                ps.setDouble(15, node.path("total_amount").asDouble(0));
                ps.setString(16, node.path("status").asText());
                ps.setTimestamp(17, parseTimestamp(node.path("sale_timestamp").asText()));
                ps.setString(18, traceId);

                int inserted = ps.executeUpdate();
                if (inserted > 0) {
                    emitLineage(traceId, saleId, source, "storage", "inserted");
                }
            }
        } catch (Exception e) {
            System.err.println("Insert error: " + e.getMessage());
            closeDb();
        }
    }

    private static void emitLineage(String traceId, String saleId, String source, String stage, String eventType) {
        if (!lineageEnabled || lineageProducer == null || traceId == null || traceId.isEmpty()) return;
        try {
            ObjectNode event = mapper.createObjectNode();
            event.put("trace_id", traceId);
            event.put("sale_id", saleId);
            event.put("stage", stage);
            event.put("component", COMPONENT_NAME);
            event.put("event_type", eventType);
            event.put("timestamp", Instant.now().toString());
            event.put("source_topic", INPUT_TOPIC);
            event.put("target_topic", OUTPUT_TOPIC);
            lineageProducer.send(new ProducerRecord<>(LINEAGE_TOPIC, traceId, mapper.writeValueAsString(event)));
        } catch (Exception ignored) {}
    }

    private static Timestamp parseTimestamp(String value) {
        try {
            return Timestamp.from(Instant.parse(value));
        } catch (Exception e) {
            LocalDateTime ldt = LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return Timestamp.from(ldt.toInstant(ZoneOffset.UTC));
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
        return System.getenv().getOrDefault(key, def);
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
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static void ensureTopic(String broker, String topic) {
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", broker))) {
            if (!admin.listTopics().names().get().contains(topic)) {
                admin.createTopics(List.of(new NewTopic(topic, 1, (short) 1))).all().get();
            }
        } catch (Exception ignored) {}
    }
}
