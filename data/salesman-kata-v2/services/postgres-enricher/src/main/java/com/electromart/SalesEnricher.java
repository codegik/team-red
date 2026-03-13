package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.KStream;

import java.time.Instant;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class SalesEnricher {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static String SALES_TOPIC;
    private static String PRODUCTS_TOPIC;
    private static String SALESMEN_TOPIC;
    private static String STORES_TOPIC;
    private static String OUTPUT_TOPIC;

    public static void main(String[] args) throws Exception {
        String broker = System.getenv().getOrDefault("KAFKA_BROKER", "kafka:9092");
        String debeziumPrefix = System.getenv().getOrDefault("DEBEZIUM_PREFIX", "electromart.public");
        SALES_TOPIC = System.getenv().getOrDefault("TOPIC_CDC_SALES", debeziumPrefix + ".sales");
        PRODUCTS_TOPIC = System.getenv().getOrDefault("TOPIC_CDC_PRODUCTS", debeziumPrefix + ".products");
        SALESMEN_TOPIC = System.getenv().getOrDefault("TOPIC_CDC_SALESMEN", debeziumPrefix + ".salesmen");
        STORES_TOPIC = System.getenv().getOrDefault("TOPIC_CDC_STORES", debeziumPrefix + ".stores");
        OUTPUT_TOPIC = System.getenv().getOrDefault("TOPIC_RAW_SALES", System.getenv().getOrDefault("OUTPUT_TOPIC", "raw-sales"));

        validateTopicConfig(Map.of(
            "TOPIC_CDC_SALES", SALES_TOPIC, "TOPIC_CDC_PRODUCTS", PRODUCTS_TOPIC,
            "TOPIC_CDC_SALESMEN", SALESMEN_TOPIC, "TOPIC_CDC_STORES", STORES_TOPIC,
            "TOPIC_RAW_SALES", OUTPUT_TOPIC
        ));

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "postgres-enricher");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);

        waitForTopics(broker);

        StreamsBuilder builder = new StreamsBuilder();

        GlobalKTable<String, String> products = builder.globalTable(PRODUCTS_TOPIC);
        GlobalKTable<String, String> salesmen = builder.globalTable(SALESMEN_TOPIC);
        GlobalKTable<String, String> stores = builder.globalTable(STORES_TOPIC);

        KStream<String, String> sales = builder.stream(SALES_TOPIC);

        KStream<String, String> enriched = sales
            .filter((key, value) -> value != null)
            .mapValues(SalesEnricher::addTraceId)
            .join(products,
                (key, sale) -> toKey("id", sale, "product_id"),
                SalesEnricher::withProduct)
            .join(salesmen,
                (key, sale) -> toKey("id", sale, "salesman_id"),
                SalesEnricher::withSalesman)
            .join(stores,
                (key, sale) -> toKey("id", sale, "store_id"),
                SalesEnricher::withStore)
            .mapValues(SalesEnricher::normalizeCanonical);

        enriched.to(OUTPUT_TOPIC);

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        System.out.println("Starting Sales Enricher - output topic: " + OUTPUT_TOPIC);
        streams.start();
    }

    private static String addTraceId(String json) {
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(json);
            if (!node.has("trace_id")) {
                node.put("trace_id", UUID.randomUUID().toString().substring(0, 8));
            }
            node.put("source", "postgres");
            node.put("source_version", "v1");
            node.put("ingested_at", Instant.now().toString());
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return json;
        }
    }

    private static String toKey(String pkField, String json, String fkField) {
        try {
            JsonNode node = mapper.readTree(json);
            int fk = node.get(fkField).asInt();
            return "{\"" + pkField + "\":" + fk + "}";
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String withProduct(String saleJson, String productJson) {
        try {
            ObjectNode sale = (ObjectNode) mapper.readTree(saleJson);
            JsonNode product = mapper.readTree(productJson);
            sale.put("product_code", product.get("code").asText());
            sale.put("product_name", product.get("name").asText());
            sale.put("category", product.get("category").asText());
            sale.put("brand", product.get("brand").asText());
            return mapper.writeValueAsString(sale);
        } catch (Exception e) {
            return saleJson;
        }
    }

    private static String withSalesman(String saleJson, String salesmanJson) {
        try {
            ObjectNode sale = (ObjectNode) mapper.readTree(saleJson);
            JsonNode salesman = mapper.readTree(salesmanJson);
            sale.put("salesman_name", salesman.get("name").asText());
            sale.put("salesman_email", salesman.get("email").asText());
            sale.put("region", salesman.get("region").asText());
            return mapper.writeValueAsString(sale);
        } catch (Exception e) {
            return saleJson;
        }
    }

    private static String withStore(String saleJson, String storeJson) {
        try {
            ObjectNode sale = (ObjectNode) mapper.readTree(saleJson);
            JsonNode store = mapper.readTree(storeJson);
            sale.put("store_name", store.get("name").asText());
            sale.put("city", store.get("city").asText());
            sale.put("country", store.get("country").asText());
            sale.put("store_type", store.get("store_type").asText());
            sale.remove("product_id");
            sale.remove("salesman_id");
            sale.remove("store_id");
            return mapper.writeValueAsString(sale);
        } catch (Exception e) {
            return saleJson;
        }
    }

    /**
     * Converts a CDC-enriched postgres record to the canonical SaleEvent schema:
     *  - numeric sale_id (auto-increment PK)  → "PG-{id}"
     *  - sale_timestamp as epoch ms (Debezium connect mode) → ISO-8601 string
     *  - strips internal DB audit columns (created_at, updated_at)
     */
    private static String normalizeCanonical(String json) {
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(json);

            JsonNode saleId = node.get("sale_id");
            if (saleId != null && saleId.isNumber()) {
                node.put("sale_id", "PG-" + saleId.asLong());
            }

            JsonNode ts = node.get("sale_timestamp");
            if (ts != null && ts.isNumber()) {
                node.put("sale_timestamp", Instant.ofEpochMilli(ts.asLong()).toString());
            }

            node.remove("created_at");
            node.remove("updated_at");

            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return json;
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

    private static void waitForTopics(String broker) throws InterruptedException, ExecutionException {
        Set<String> required = Set.of(SALES_TOPIC, PRODUCTS_TOPIC, SALESMEN_TOPIC, STORES_TOPIC);

        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", broker))) {
            while (true) {
                Set<String> existing = admin.listTopics().names().get();
                if (existing.containsAll(required)) {
                    System.out.println("All Debezium topics found");
                    return;
                }
                Thread.sleep(5000);
            }
        }
    }

}
