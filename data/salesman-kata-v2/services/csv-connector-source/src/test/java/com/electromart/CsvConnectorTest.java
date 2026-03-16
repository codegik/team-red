package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvConnectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void csvLineToJsonParsesValidLine() throws Exception {
        String json = invokeCsvLineToJson("S-1,P-1,Notebook,Electronics,BrandX,Ana,ana@example.com,NE,Store A,Fortaleza,retail,2,10.5,21.0,paid,2026-03-13T10:15:30Z");

        JsonNode node = MAPPER.readTree(json);
        assertEquals("S-1", node.get("sale_id").asText());
        assertEquals(2, node.get("quantity").asInt());
        assertEquals(10.5, node.get("unit_price").asDouble());
        assertEquals(21.0, node.get("total_amount").asDouble());
        assertNotNull(node.get("picked_up_at").asText());
    }

    @Test
    void csvLineToJsonReturnsNullForInvalidNumericValue() throws Exception {
        String json = withSuppressedErr(
            () -> invokeCsvLineToJson("S-1,P-1,Notebook,Electronics,BrandX,Ana,ana@example.com,NE,Store A,Fortaleza,retail,two,10.5,21.0,paid,2026-03-13T10:15:30Z")
        );

        assertNull(json);
    }

    @Test
    void validateTopicConfigRejectsInvalidTopicNames() throws Exception {
        Method method = CsvConnector.class.getDeclaredMethod("validateTopicConfig", Map.class);
        method.setAccessible(true);
        try {
            method.invoke(null, Map.of("TOPIC_OUTPUT", "bad topic"));
        } catch (Exception error) {
            assertTrue(error.getCause() instanceof IllegalArgumentException);
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException");
    }

    private String invokeCsvLineToJson(String line) throws Exception {
        Method method = CsvConnector.class.getDeclaredMethod("csvLineToJson", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, line);
    }

    private <T> T withSuppressedErr(ThrowingSupplier<T> action) throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        System.setErr(new PrintStream(sink));
        try {
            return action.get();
        } finally {
            System.setErr(original);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
