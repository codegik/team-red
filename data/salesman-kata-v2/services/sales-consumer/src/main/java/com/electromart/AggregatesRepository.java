package com.electromart;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AggregatesRepository implements AggregatesService {

    public static final AggregatesRepository INSTANCE = new AggregatesRepository();

    private AggregatesRepository() {
    }

    @Override
    public void ping(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
            stmt.execute();
        }
    }

    @Override
    public AggregateResult queryTopCities(Connection conn, String from, String to, int limit) throws SQLException {
        List<Object> params = new ArrayList<>();
        String filter = buildTimeFilter(from, to, params);

        if (!filter.isBlank()) {
            params.add(limit);
            String sql = """
                SELECT
                  city,
                  MAX(region) AS region,
                  SUM(total_revenue) AS total_revenue,
                  SUM(total_quantity) AS total_quantity,
                  SUM(total_sales) AS total_sales,
                  MIN(bucket) AS first_bucket,
                  MAX(bucket) AS last_bucket
                FROM top_cities
                %s
                GROUP BY city
                ORDER BY total_revenue DESC, city ASC
                LIMIT ?
                """.formatted(filter);
            return new AggregateResult("range", query(conn, sql, params));
        }

        String sql = """
            WITH latest_bucket AS (
              SELECT MAX(bucket) AS bucket
              FROM top_cities
            )
            SELECT
              tc.bucket,
              tc.city,
              tc.region,
              tc.total_revenue,
              tc.total_quantity,
              tc.total_sales
            FROM top_cities tc
            JOIN latest_bucket lb ON tc.bucket = lb.bucket
            ORDER BY tc.total_revenue DESC, tc.city ASC
            LIMIT ?
            """;
        return new AggregateResult("latest", query(conn, sql, List.of(limit)));
    }

    @Override
    public AggregateResult queryTopSalesmen(Connection conn, String from, String to, int limit) throws SQLException {
        List<Object> params = new ArrayList<>();
        String filter = buildTimeFilter(from, to, params);

        if (!filter.isBlank()) {
            params.add(limit);
            String sql = """
                SELECT
                  salesman_name,
                  salesman_email,
                  MAX(region) AS region,
                  SUM(total_revenue) AS total_revenue,
                  SUM(total_quantity) AS total_quantity,
                  SUM(total_sales) AS total_sales,
                  MIN(bucket) AS first_bucket,
                  MAX(bucket) AS last_bucket
                FROM top_salesmen
                %s
                GROUP BY salesman_name, salesman_email
                ORDER BY total_revenue DESC, salesman_name ASC
                LIMIT ?
                """.formatted(filter);
            return new AggregateResult("range", query(conn, sql, params));
        }

        String sql = """
            WITH latest_bucket AS (
              SELECT MAX(bucket) AS bucket
              FROM top_salesmen
            )
            SELECT
              ts.bucket,
              ts.salesman_name,
              ts.salesman_email,
              ts.region,
              ts.total_revenue,
              ts.total_quantity,
              ts.total_sales
            FROM top_salesmen ts
            JOIN latest_bucket lb ON ts.bucket = lb.bucket
            ORDER BY ts.total_revenue DESC, ts.salesman_name ASC
            LIMIT ?
            """;
        return new AggregateResult("latest", query(conn, sql, List.of(limit)));
    }

    private static String buildTimeFilter(String from, String to, List<Object> params) {
        List<String> clauses = new ArrayList<>();

        if (from != null) {
            params.add(from);
            clauses.add("bucket >= ?");
        }

        if (to != null) {
            params.add(to);
            clauses.add("bucket <= ?");
        }

        if (clauses.isEmpty()) {
            return "";
        }

        return "WHERE " + String.join(" AND ", clauses);
    }

    private static List<Map<String, Object>> query(Connection conn, String sql, List<Object> params) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return toRows(rs);
            }
        }
    }

    private static List<Map<String, Object>> toRows(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columns; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }

        return rows;
    }
}
