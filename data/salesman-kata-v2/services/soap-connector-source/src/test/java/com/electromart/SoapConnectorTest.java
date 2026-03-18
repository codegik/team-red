package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoapConnectorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void buildRequestContainsPageSize() {
        String xml = SoapConnector.buildRequest(50);

        assertTrue(xml.contains("<pageSize>50</pageSize>"));
    }

    @Test
    void recordToJsonParsesSoapRecord() throws Exception {
        Element record = parseRecord("""
            <sale:record xmlns:sale="http://electromart.com/sales">
              <sale:saleId>S-1</sale:saleId>
              <sale:productCode>P-1</sale:productCode>
              <sale:productName>Notebook</sale:productName>
              <sale:category>Electronics</sale:category>
              <sale:brand>BrandX</sale:brand>
              <sale:salesmanName>Ana</sale:salesmanName>
              <sale:salesmanEmail>ana@example.com</sale:salesmanEmail>
              <sale:region>NE</sale:region>
              <sale:storeName>Store A</sale:storeName>
              <sale:city>Fortaleza</sale:city>
              <sale:storeType>retail</sale:storeType>
              <sale:quantity>2</sale:quantity>
              <sale:unitPrice>10.5</sale:unitPrice>
              <sale:totalAmount>21.0</sale:totalAmount>
              <sale:status>paid</sale:status>
              <sale:saleTimestamp>2026-03-13T10:15:30Z</sale:saleTimestamp>
            </sale:record>
            """);

        String json = invokeRecordToJson(record);

        JsonNode node = MAPPER.readTree(json);
        assertEquals("S-1", node.get("sale_id").asText());
        assertEquals(2, node.get("quantity").asInt());
        assertEquals(21.0, node.get("total_amount").asDouble());
        assertNotNull(node.get("picked_up_at").asText());
    }

    @Test
    void recordToJsonReturnsNullForInvalidNumericValue() throws Exception {
        Element record = parseRecord("""
            <sale:record xmlns:sale="http://electromart.com/sales">
              <sale:saleId>S-1</sale:saleId>
              <sale:quantity>two</sale:quantity>
            </sale:record>
            """);

        assertNull(withSuppressedErr(() -> invokeRecordToJson(record)));
    }

    @Test
    void validateTopicConfigRejectsInvalidTopicNames() throws Exception {
        Method method = SoapConnector.class.getDeclaredMethod("validateTopicConfig", Map.class);
        method.setAccessible(true);
        try {
            method.invoke(null, Map.of("TOPIC_OUTPUT", "bad topic"));
        } catch (Exception error) {
            assertTrue(error.getCause() instanceof IllegalArgumentException);
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException");
    }

    private String invokeRecordToJson(Element record) throws Exception {
        Method method = SoapConnector.class.getDeclaredMethod("recordToJson", Element.class);
        method.setAccessible(true);
        return (String) method.invoke(null, record);
    }

    private Element parseRecord(String xml) throws Exception {
        Method parseXml = SoapConnector.class.getDeclaredMethod("parseXml", String.class);
        parseXml.setAccessible(true);
        Document document = (Document) parseXml.invoke(null, xml);
        return document.getDocumentElement();
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
