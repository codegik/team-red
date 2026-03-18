package com.electromart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import com.sun.net.httpserver.HttpServer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class SoapConnector {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static SchemaValidator schemaValidator;
    private static KafkaProducer<String, String> producer;
    private static String dlqTopic;
    private static final AtomicLong recordsSentToDlq = new AtomicLong(0);

    public static void main(String[] args) throws Exception {
        String broker = System.getenv().getOrDefault("KAFKA_BROKER", "kafka:9092");
        String topic = env("TOPIC_OUTPUT", "raw_soap");
        validateTopicConfig(Map.of("TOPIC_OUTPUT", topic));
        String soapUrl = System.getenv().getOrDefault("SOAP_URL", "http://soap-service:8080/sales");
        int pageSize = Integer.parseInt(System.getenv().getOrDefault("PAGE_SIZE", "100"));
        long pollInterval = Long.parseLong(System.getenv().getOrDefault("POLL_INTERVAL", "5000"));

        System.out.println("SOAP Connector starting...");
        System.out.printf("Config: broker=%s | topic=%s%n", broker, topic);

        dlqTopic = env("TOPIC_DLQ", "raw_soap-dlq");
        waitForKafka(broker);
        ensureTopic(broker, topic);
        ensureTopic(broker, dlqTopic);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        producer = new KafkaProducer<>(props);
        Runtime.getRuntime().addShutdownHook(new Thread(producer::close));
        startHealthServer(8087);

        schemaValidator = SchemaValidator.tryCreate(
            env("SCHEMA_REGISTRY_URL", "http://schema-registry:8081"),
            "raw_soap-value",
            Integer.parseInt(env("SCHEMA_REGISTRY_VERSION", "1")));

        waitForSoapService(soapUrl);

        while (true) {
            try {
                pollAndPublish(topic, soapUrl, pageSize);
            } catch (Exception e) {
                System.err.println("Poll error: " + e.getMessage());
            }
            Thread.sleep(pollInterval);
        }
    }

    private static void pollAndPublish(String topic,
                                          String soapUrl, int pageSize) throws Exception {
        String requestXml = buildRequest(pageSize);
        String responseXml = postSoap(soapUrl, requestXml);

        Document doc = parseXml(responseXml);

        NodeList records = doc.getElementsByTagName("sale:record");
        if (records.getLength() == 0) return;

        int totalPublished = 0;

        for (int i = 0; i < records.getLength(); i++) {
            Element record = (Element) records.item(i);
            String json = recordToJson(record);
            if (json == null) continue;
            String saleId = getTagValue(record, "sale:saleId");
            producer.send(new ProducerRecord<>(topic, saleId, json));
            totalPublished++;
        }

        producer.flush();

        if (totalPublished > 0) {
            System.out.printf("[%s] Published to topic \"%s\" | %d records%n",
                java.time.Instant.now(), topic, totalPublished);
        }
    }

    static String buildRequest(int pageSize) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                              xmlns:sale="http://electromart.com/sales">
              <soapenv:Body>
                <sale:GetSalesRequest>
                  <pageSize>%d</pageSize>
                </sale:GetSalesRequest>
              </soapenv:Body>
            </soapenv:Envelope>""", pageSize);
    }

    private static String postSoap(String url, String xml) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "text/xml");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(xml.getBytes(StandardCharsets.UTF_8));
        }

        try (InputStream is = conn.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Document parseXml(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private static String getTagValue(Node parent, String tagName) {
        if (parent instanceof Document doc) {
            NodeList nodes = doc.getElementsByTagName(tagName);
            return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : null;
        } else if (parent instanceof Element el) {
            NodeList nodes = el.getElementsByTagName(tagName);
            return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : null;
        }
        return null;
    }

    private static final Map<String, String> FIELD_MAP = Map.ofEntries(
        Map.entry("sale:saleId", "sale_id"),
        Map.entry("sale:productCode", "product_code"),
        Map.entry("sale:productName", "product_name"),
        Map.entry("sale:category", "category"),
        Map.entry("sale:brand", "brand"),
        Map.entry("sale:salesmanName", "salesman_name"),
        Map.entry("sale:salesmanEmail", "salesman_email"),
        Map.entry("sale:region", "region"),
        Map.entry("sale:storeName", "store_name"),
        Map.entry("sale:city", "city"),
        Map.entry("sale:storeType", "store_type"),
        Map.entry("sale:quantity", "quantity"),
        Map.entry("sale:unitPrice", "unit_price"),
        Map.entry("sale:totalAmount", "total_amount"),
        Map.entry("sale:status", "status"),
        Map.entry("sale:saleTimestamp", "sale_timestamp")
    );

    private static String recordToJson(Element record) throws Exception {
        String saleId = getTagValue(record, "sale:saleId");
        if (saleId == null) saleId = "unknown";

        ObjectNode node = mapper.createObjectNode();
        try {
            for (var entry : FIELD_MAP.entrySet()) {
                String value = getTagValue(record, entry.getKey());
                if (value == null) continue;

                String jsonField = entry.getValue();
                if (jsonField.equals("quantity")) {
                    node.put(jsonField, Integer.parseInt(value));
                } else if (jsonField.equals("unit_price") || jsonField.equals("total_amount")) {
                    node.put(jsonField, Double.parseDouble(value));
                } else {
                    node.put(jsonField, value);
                }
            }
        } catch (NumberFormatException e) {
            publishToDlq(saleId, elementToXml(record), "PARSE_ERROR", "Invalid numeric value: " + e.getMessage());
            return null;
        }

        if (schemaValidator != null && !schemaValidator.isValid(node)) {
            publishToDlq(saleId, elementToXml(record), "SCHEMA_VALIDATION", "Schema validation failed");
            return null;
        }
        node.put("picked_up_at", Instant.now().toString());
        return mapper.writeValueAsString(node);
    }

    private static String elementToXml(Element element) {
        try {
            StringWriter writer = new StringWriter();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return element.getTagName();
        }
    }

    private static void publishToDlq(String key, String raw, String errorType, String errorMessage) {
        try {
            ObjectNode envelope = mapper.createObjectNode();
            envelope.put("error", errorMessage);
            envelope.put("error_type", errorType);
            envelope.put("source", "soap");
            envelope.put("picked_up_at", Instant.now().toString());
            envelope.put("raw", raw);
            producer.send(new ProducerRecord<>(dlqTopic, key, mapper.writeValueAsString(envelope)));
            recordsSentToDlq.incrementAndGet();
        } catch (Exception e) {
            System.err.printf("Failed to publish to DLQ: %s%n", e.getMessage());
        }
    }

    private static void startHealthServer(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/health", ex -> {
                String resp = String.format(
                    "{\"status\":\"healthy\",\"records_sent_to_dlq\":%d,\"schema_validation_enabled\":%b}",
                    recordsSentToDlq.get(), schemaValidator != null);
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

    private static String env(String key, String def) {
        return System.getenv().getOrDefault(key, def);
    }

    private static void waitForKafka(String broker) throws Exception {
        try (AdminClient admin = AdminClient.create(Map.of("bootstrap.servers", broker))) {
            while (true) {
                try {
                    admin.listTopics().names().get();
                    System.out.println("Kafka is ready");
                    return;
                } catch (Exception e) {
                    Thread.sleep(3000);
                }
            }
        }
    }

    private static void waitForSoapService(String url) {
        String healthUrl = url.replace("/sales", "/health");
        while (true) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(healthUrl).openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                if (conn.getResponseCode() == 200) {
                    System.out.println("SOAP service is ready");
                    return;
                }
            } catch (Exception ignored) {}
            try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
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
