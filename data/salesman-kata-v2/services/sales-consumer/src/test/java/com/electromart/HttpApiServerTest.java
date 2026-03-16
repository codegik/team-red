package com.electromart;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpApiServerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private HttpApiServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void healthEndpointReturnsUp() throws Exception {
        FakeAggregatesService service = new FakeAggregatesService();
        server = new HttpApiServer(
            new DatabaseConfig("localhost", "5432", "sales", "sales123", "salesdb"),
            0,
            HttpApiServerTest::fakeConnection,
            service
        );
        server.start();

        HttpResponse<String> response = get("/health");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = readJson(response.body());
        assertEquals("UP", json.get("status"));
        assertEquals("sales-consumer", json.get("service"));
        assertEquals(1, service.pingCalls);
    }

    @Test
    void topCitiesEndpointReturnsAggregatedPayload() throws Exception {
        FakeAggregatesService service = new FakeAggregatesService();
        service.topCitiesResult = new AggregateResult(
            "range",
            List.of(Map.of("city", "Fortaleza", "total_revenue", 1200.0))
        );

        server = new HttpApiServer(
            new DatabaseConfig("localhost", "5432", "sales", "sales123", "salesdb"),
            0,
            HttpApiServerTest::fakeConnection,
            service
        );
        server.start();

        HttpResponse<String> response = get("/api/aggregates/top-sales-per-city?from=2026-03-13T00:00:00Z&to=2026-03-13T23:59:59Z&limit=7");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = readJson(response.body());
        assertEquals("top_cities", json.get("source"));
        assertEquals("range", json.get("mode"));
        assertEquals(1, json.get("count"));

        Map<String, Object> filters = castMap(json.get("filters"));
        assertEquals("2026-03-13T00:00:00Z", filters.get("from"));
        assertEquals("2026-03-13T23:59:59Z", filters.get("to"));
        assertEquals(7, filters.get("limit"));
        assertEquals("2026-03-13T00:00:00Z", service.lastFrom);
        assertEquals("2026-03-13T23:59:59Z", service.lastTo);
        assertEquals(7, service.lastLimit);
    }

    @Test
    void summaryEndpointUsesDefaultLimitsWhenQueryParamsAreMissing() throws Exception {
        FakeAggregatesService service = new FakeAggregatesService();
        service.topCitiesResult = new AggregateResult("latest", List.of(Map.of("city", "Recife")));
        service.topSalesmenResult = new AggregateResult("latest", List.of(Map.of("salesman_name", "Ana")));

        server = new HttpApiServer(
            new DatabaseConfig("localhost", "5432", "sales", "sales123", "salesdb"),
            0,
            HttpApiServerTest::fakeConnection,
            service
        );
        server.start();

        HttpResponse<String> response = get("/api/aggregates/summary");

        assertEquals(200, response.statusCode());
        Map<String, Object> json = readJson(response.body());
        Map<String, Object> filters = castMap(json.get("filters"));
        assertNull(filters.get("from"));
        assertNull(filters.get("to"));
        assertEquals(5, filters.get("cityLimit"));
        assertEquals(5, filters.get("salesmanLimit"));
        assertEquals(5, service.lastCityLimit);
        assertEquals(5, service.lastSalesmanLimit);
    }

    @Test
    void invalidDateReturnsBadRequest() throws Exception {
        server = new HttpApiServer(
            new DatabaseConfig("localhost", "5432", "sales", "sales123", "salesdb"),
            0,
            HttpApiServerTest::fakeConnection,
            new FakeAggregatesService()
        );
        server.start();

        HttpResponse<String> response = get("/api/aggregates/top-salesman-country?from=invalid");

        assertEquals(400, response.statusCode());
        Map<String, Object> json = readJson(response.body());
        assertEquals("Invalid 'from' date. Use ISO-8601.", json.get("error"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + server.getPort() + path))
            .GET()
            .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, Object> readJson(String json) throws Exception {
        return MAPPER.readValue(json, new TypeReference<>() {
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static Connection fakeConnection() {
        return (Connection) Proxy.newProxyInstance(
            HttpApiServerTest.class.getClassLoader(),
            new Class[]{Connection.class},
            (proxy, method, args) -> {
                if ("close".equals(method.getName())) {
                    return null;
                }
                if ("isValid".equals(method.getName())) {
                    return true;
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class FakeAggregatesService implements AggregatesService {
        private int pingCalls;
        private String lastFrom;
        private String lastTo;
        private int lastLimit;
        private int lastCityLimit;
        private int lastSalesmanLimit;
        private AggregateResult topCitiesResult = new AggregateResult("latest", List.of());
        private AggregateResult topSalesmenResult = new AggregateResult("latest", List.of());

        @Override
        public void ping(Connection conn) {
            pingCalls++;
        }

        @Override
        public AggregateResult queryTopCities(Connection conn, String from, String to, int limit) {
            lastFrom = from;
            lastTo = to;
            lastLimit = limit;
            lastCityLimit = limit;
            return topCitiesResult;
        }

        @Override
        public AggregateResult queryTopSalesmen(Connection conn, String from, String to, int limit) {
            lastFrom = from;
            lastTo = to;
            lastLimit = limit;
            lastSalesmanLimit = limit;
            return topSalesmenResult;
        }
    }
}
