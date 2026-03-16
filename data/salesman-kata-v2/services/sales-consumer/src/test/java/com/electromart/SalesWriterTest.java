package com.electromart;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesWriterTest {

    @Test
    void insertSaleMapsFieldsIntoPreparedStatement() throws Exception {
        RecordingPreparedStatement recorder = new RecordingPreparedStatement();
        Connection connection = connectionReturning(recorder.statement());

        SalesWriter.insertSale(connection, """
            {
              "sale_id":"sale-1",
              "source":"csv",
              "product_code":"P-10",
              "product_name":"Notebook",
              "quantity":3,
              "unit_price":100.5,
              "total_amount":301.5,
              "sale_timestamp":"2026-03-13T10:15:30Z",
              "trace_id":"trace-1"
            }
            """);

        assertEquals("sale-1", recorder.parameters().get(1));
        assertEquals("csv", recorder.parameters().get(2));
        assertEquals("P-10", recorder.parameters().get(3));
        assertEquals("Notebook", recorder.parameters().get(4));
        assertEquals(3, recorder.parameters().get(13));
        assertEquals(100.5, recorder.parameters().get(14));
        assertEquals(301.5, recorder.parameters().get(15));
        assertEquals(Timestamp.from(Instant.parse("2026-03-13T10:15:30Z")), recorder.parameters().get(17));
        assertEquals("trace-1", recorder.parameters().get(18));
        assertTrue(recorder.executed());
    }

    @Test
    void insertSaleFallsBackToNowForInvalidTimestamp() throws Exception {
        RecordingPreparedStatement recorder = new RecordingPreparedStatement();
        Connection connection = connectionReturning(recorder.statement());
        Instant before = Instant.now().minusSeconds(1);

        SalesWriter.insertSale(connection, """
            {
              "sale_id":"sale-2",
              "source":"soap",
              "total_amount":45.0,
              "sale_timestamp":"invalid-date"
            }
            """);

        Timestamp timestamp = (Timestamp) recorder.parameters().get(17);
        assertTrue(!timestamp.toInstant().isBefore(before));
        assertTrue(recorder.executed());
    }

    private Connection connectionReturning(PreparedStatement statement) {
        return (Connection) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[]{Connection.class},
            (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    return statement;
                }
                if ("close".equals(method.getName())) {
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private Object defaultValue(Class<?> returnType) {
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

    private static final class RecordingPreparedStatement {
        private final Map<Integer, Object> parameters = new HashMap<>();
        private boolean executed;

        PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{PreparedStatement.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setString", "setInt", "setDouble", "setTimestamp" -> {
                            parameters.put((Integer) args[0], args[1]);
                            return null;
                        }
                        case "executeUpdate" -> {
                            executed = true;
                            return 1;
                        }
                        case "close" -> {
                            return null;
                        }
                        default -> {
                            return defaultValue(method.getReturnType());
                        }
                    }
                }
            );
        }

        Map<Integer, Object> parameters() {
            return parameters;
        }

        boolean executed() {
            return executed;
        }

        private Object defaultValue(Class<?> returnType) {
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
    }
}
