package com.electromart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class HttpApiServer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DatabaseConfig databaseConfig;
    private final ConnectionProvider connectionProvider;
    private final AggregatesService aggregatesService;
    private final int port;
    private HttpServer server;

    public HttpApiServer(DatabaseConfig databaseConfig, int port) {
        this(databaseConfig, port, () -> Database.connect(databaseConfig), AggregatesRepository.INSTANCE);
    }

    HttpApiServer(
        DatabaseConfig databaseConfig,
        int port,
        ConnectionProvider connectionProvider,
        AggregatesService aggregatesService
    ) {
        this.databaseConfig = databaseConfig;
        this.port = port;
        this.connectionProvider = connectionProvider;
        this.aggregatesService = aggregatesService;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/health", this::handleHealth);
            server.createContext("/api/aggregates/top-sales-per-city", this::handleTopCities);
            server.createContext("/api/aggregates/top-salesman-country", this::handleTopSalesmen);
            server.createContext("/api/aggregates/summary", this::handleSummary);
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.printf("HTTP API listening on port %d%n", port);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start HTTP API", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    int getPort() {
        if (server == null) {
            return port;
        }

        return server.getAddress().getPort();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try (Connection conn = connectionProvider.get()) {
            aggregatesService.ping(conn);
            writeJson(exchange, 200, Map.of(
                "status", "UP",
                "service", "sales-consumer",
                "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            handleError(exchange, e);
        }
    }

    private void handleTopCities(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try (Connection conn = connectionProvider.get()) {
            QueryParams params = QueryParams.from(exchange);
            AggregateResult data = aggregatesService.queryTopCities(
                conn,
                params.from(),
                params.to(),
                params.limitOrDefault("limit", 10)
            );
            Map<String, Object> filters = new LinkedHashMap<>();
            filters.put("from", params.from());
            filters.put("to", params.to());
            filters.put("limit", params.limitOrDefault("limit", 10));

            writeJson(exchange, 200, Map.of(
                "source", "top_cities",
                "mode", data.mode(),
                "filters", filters,
                "count", data.rows().size(),
                "items", data.rows()
            ));
        } catch (Exception e) {
            handleError(exchange, e);
        }
    }

    private void handleTopSalesmen(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try (Connection conn = connectionProvider.get()) {
            QueryParams params = QueryParams.from(exchange);
            AggregateResult data = aggregatesService.queryTopSalesmen(
                conn,
                params.from(),
                params.to(),
                params.limitOrDefault("limit", 10)
            );
            Map<String, Object> filters = new LinkedHashMap<>();
            filters.put("from", params.from());
            filters.put("to", params.to());
            filters.put("limit", params.limitOrDefault("limit", 10));

            writeJson(exchange, 200, Map.of(
                "source", "top_salesmen",
                "mode", data.mode(),
                "filters", filters,
                "count", data.rows().size(),
                "items", data.rows()
            ));
        } catch (Exception e) {
            handleError(exchange, e);
        }
    }

    private void handleSummary(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }

        try (Connection conn = connectionProvider.get()) {
            QueryParams params = QueryParams.from(exchange);
            int cityLimit = params.limitOrDefault("cityLimit", 5);
            int salesmanLimit = params.limitOrDefault("salesmanLimit", 5);
            AggregateResult cities = aggregatesService.queryTopCities(conn, params.from(), params.to(), cityLimit);
            AggregateResult salesmen = aggregatesService.queryTopSalesmen(conn, params.from(), params.to(), salesmanLimit);
            Map<String, Object> filters = new LinkedHashMap<>();
            filters.put("from", params.from());
            filters.put("to", params.to());
            filters.put("cityLimit", cityLimit);
            filters.put("salesmanLimit", salesmanLimit);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("filters", filters);
            response.put("topSalesPerCity", Map.of(
                "source", "top_cities",
                "mode", cities.mode(),
                "count", cities.rows().size(),
                "items", cities.rows()
            ));
            response.put("topSalesmanCountry", Map.of(
                "source", "top_salesmen",
                "mode", salesmen.mode(),
                "count", salesmen.rows().size(),
                "items", salesmen.rows()
            ));

            writeJson(exchange, 200, response);
        } catch (Exception e) {
            handleError(exchange, e);
        }
    }

    private void handleError(HttpExchange exchange, Exception error) throws IOException {
        if (error instanceof ApiException apiException) {
            writeJson(exchange, apiException.statusCode(), Map.of("error", apiException.getMessage()));
            return;
        }

        error.printStackTrace(System.err);
        writeJson(exchange, 500, Map.of("error", "Internal server error"));
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] response = MAPPER.writeValueAsBytes(payload);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    private record QueryParams(Map<String, String> values) {

        static QueryParams from(HttpExchange exchange) {
            String rawQuery = exchange.getRequestURI().getRawQuery();
            Map<String, String> values = new LinkedHashMap<>();

            if (rawQuery != null && !rawQuery.isBlank()) {
                for (String pair : rawQuery.split("&")) {
                    if (pair.isBlank()) {
                        continue;
                    }

                    String[] parts = pair.split("=", 2);
                    String key = decode(parts[0]);
                    String value = parts.length > 1 ? decode(parts[1]) : "";
                    values.put(key, value);
                }
            }

            return new QueryParams(values);
        }

        String from() {
            return parseIsoDate(values.get("from"), "from");
        }

        String to() {
            return parseIsoDate(values.get("to"), "to");
        }

        int limitOrDefault(String key, int fallback) {
            return parseLimit(values.get(key), fallback, 100);
        }

        private static String decode(String value) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }

        private static int parseLimit(String value, int fallback, int max) {
            if (value == null || value.isBlank()) {
                return fallback;
            }

            try {
                int parsed = Integer.parseInt(value);
                if (parsed <= 0) {
                    return fallback;
                }
                return Math.min(parsed, max);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }

        private static String parseIsoDate(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                return null;
            }

            try {
                return Instant.parse(value).toString();
            } catch (Exception ignored) {
                throw new ApiException(400, "Invalid '" + fieldName + "' date. Use ISO-8601.");
            }
        }
    }
}
