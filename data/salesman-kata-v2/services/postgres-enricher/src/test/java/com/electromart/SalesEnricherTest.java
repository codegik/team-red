package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesEnricherTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void normalizeCanonicalConvertsNumericFieldsAndRemovesAuditColumns() throws Exception {
        String json = invoke("normalizeCanonical", """
            {"sale_id":12,"sale_timestamp":1741860930000,"created_at":"x","updated_at":"y"}
            """);

        JsonNode node = MAPPER.readTree(json);
        assertEquals("PG-12", node.get("sale_id").asText());
        assertEquals("2025-03-13T10:15:30Z", node.get("sale_timestamp").asText());
        assertFalse(node.has("created_at"));
        assertFalse(node.has("updated_at"));
    }

    @Test
    void addTraceIdAddsMetadataWhenMissing() throws Exception {
        String json = invoke("addTraceId", "{\"sale_id\":\"1\"}");

        JsonNode node = MAPPER.readTree(json);
        assertEquals("postgres", node.get("source").asText());
        assertEquals("v1", node.get("source_version").asText());
        assertNotNull(node.get("trace_id").asText());
        assertNotNull(node.get("ingested_at").asText());
    }

    @Test
    void wrapDlqEnvelopeAnnotatesInvalidPayload() throws Exception {
        String json = invoke("wrapDlqEnvelope", "{\"sale_id\":\"1\"}");

        JsonNode node = MAPPER.readTree(json);
        assertEquals("SCHEMA_VALIDATION", node.get("error_type").asText());
        assertEquals("postgres", node.get("source").asText());
        assertTrue(node.get("raw").asText().contains("sale_id"));
    }

    @Test
    void validateTopicConfigRejectsInvalidTopics() throws Exception {
        Method method = SalesEnricher.class.getDeclaredMethod("validateTopicConfig", Map.class);
        method.setAccessible(true);
        try {
            method.invoke(null, Map.of("TOPIC_OUTPUT", "bad topic"));
        } catch (Exception error) {
            assertTrue(error.getCause() instanceof IllegalArgumentException);
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException");
    }

    private String invoke(String methodName, String value) throws Exception {
        Method method = SalesEnricher.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, value);
    }
}
