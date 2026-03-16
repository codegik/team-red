package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesAggregatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void addSourceMetadataPopulatesMissingFields() throws Exception {
        String json = SalesAggregator.addSourceMetadata("{\"sale_id\":\"1\",\"sale_timestamp\":\"2026-03-13T10:15:30Z\",\"total_amount\":10}", "csv");

        JsonNode node = MAPPER.readTree(json);
        assertEquals("csv", node.get("source").asText());
        assertEquals("v1", node.get("source_version").asText());
        assertNotNull(node.get("trace_id").asText());
        assertNotNull(node.get("ingested_at").asText());
    }

    @Test
    void validateAcceptsCanonicalPayload() {
        boolean valid = SalesAggregator.validate("""
            {"sale_id":"1","source":"csv","sale_timestamp":"2026-03-13T10:15:30Z","total_amount":10}
            """);

        assertTrue(valid);
    }

    @Test
    void validateRejectsPayloadMissingRequiredFields() {
        boolean valid = suppressErr(() -> SalesAggregator.validate("""
            {"sale_id":"1","source":"csv","total_amount":10}
            """));

        assertFalse(valid);
    }

    private boolean suppressErr(BooleanSupplier action) {
        PrintStream original = System.err;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        System.setErr(new PrintStream(sink));
        try {
            return action.getAsBoolean();
        } finally {
            System.setErr(original);
        }
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
