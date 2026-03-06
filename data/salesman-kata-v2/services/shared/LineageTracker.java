package com.electromart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.time.Instant;
import java.util.UUID;

/**
 * Lineage Tracker - Tracks data flow through the pipeline
 * 
 * Emits events to the "lineage" Kafka topic with the following structure:
 * {
 *   "trace_id": "uuid",
 *   "sale_id": "original-sale-id",
 *   "stage": "ingestion|enrichment|aggregation|storage",
 *   "component": "csv-connector|soap-connector|postgres-connector|sales-enricher|sales-aggregator",
 *   "event_type": "received|processed|published|stored|error",
 *   "timestamp": "2024-01-15T10:32:15Z",
 *   "source_topic": "optional",
 *   "target_topic": "optional",
 *   "latency_ms": 0,
 *   "metadata": {}
 * }
 */
public class LineageTracker {

    public static final String LINEAGE_TOPIC = "lineage";

    public static final String STAGE_INGESTION = "ingestion";
    public static final String STAGE_ENRICHMENT = "enrichment";
    public static final String STAGE_AGGREGATION = "aggregation";
    public static final String STAGE_STORAGE = "storage";

    public static final String EVENT_RECEIVED = "received";
    public static final String EVENT_PROCESSED = "processed";
    public static final String EVENT_PUBLISHED = "published";
    public static final String EVENT_STORED = "stored";
    public static final String EVENT_ERROR = "error";

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static void track(KafkaProducer<String, String> producer,
                             String traceId,
                             String saleId,
                             String stage,
                             String component,
                             String eventType,
                             String sourceTopic,
                             String targetTopic,
                             long latencyMs,
                             String metadata) {
        try {
            ObjectNode event = mapper.createObjectNode();
            event.put("trace_id", traceId);
            event.put("sale_id", saleId);
            event.put("stage", stage);
            event.put("component", component);
            event.put("event_type", eventType);
            event.put("timestamp", Instant.now().toString());
            
            if (sourceTopic != null) event.put("source_topic", sourceTopic);
            if (targetTopic != null) event.put("target_topic", targetTopic);
            if (latencyMs > 0) event.put("latency_ms", latencyMs);
            if (metadata != null) event.put("metadata", metadata);

            producer.send(new ProducerRecord<>(LINEAGE_TOPIC, traceId, mapper.writeValueAsString(event)));
        } catch (Exception e) {
            System.err.println("Lineage tracking error: " + e.getMessage());
        }
    }

    public static void trackSimple(KafkaProducer<String, String> producer,
                                   String traceId,
                                   String saleId,
                                   String stage,
                                   String component,
                                   String eventType) {
        track(producer, traceId, saleId, stage, component, eventType, null, null, 0, null);
    }
}
