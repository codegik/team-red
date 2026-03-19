package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;

public final class SalesWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String INSERT_SQL = """
        INSERT INTO sales (
            sale_id, source, product_code, product_name, category, brand,
            salesman_name, salesman_email, region, store_name, city, store_type,
            quantity, unit_price, total_amount, sale_timestamp, trace_id
        ) VALUES (
            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
        ) ON CONFLICT (sale_id, sale_timestamp) DO NOTHING
        """;

    private SalesWriter() {
    }

    public static SalesWriteResult insertSale(Connection conn, String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        boolean timestampFallbackUsed = false;
        Timestamp saleTimestamp;

        try {
            saleTimestamp = Timestamp.from(Instant.parse(node.path("sale_timestamp").asText()));
        } catch (Exception e) {
            saleTimestamp = Timestamp.from(Instant.now());
            timestampFallbackUsed = true;
        }

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
            stmt.setTimestamp(16, saleTimestamp);
            stmt.setString(17, textOrNull(node, "trace_id"));

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.printf("Inserted sale %s%n", node.path("sale_id").asText());
            }

            return new SalesWriteResult(
                rows > 0,
                timestampFallbackUsed,
                node.path("sale_id").asText("unknown"),
                node.path("source").asText("unknown"),
                node.path("total_amount").asDouble(0),
                textOrNull(node, "picked_up_at")
            );
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode val = node.get(field);
        if (val == null || val.isNull() || val.asText().isBlank()) {
            return null;
        }

        return val.asText();
    }
}
