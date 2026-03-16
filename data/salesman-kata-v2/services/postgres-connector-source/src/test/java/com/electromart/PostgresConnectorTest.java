package com.electromart;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresConnectorTest {

    @Test
    void buildConnectorConfigContainsExpectedDebeziumFields() {
        ObjectNode config = PostgresConnector.buildConnectorConfig();

        assertEquals("io.debezium.connector.postgresql.PostgresConnector", config.get("connector.class").asText());
        assertEquals("postgres", config.get("database.hostname").asText());
        assertEquals("5432", config.get("database.port").asText());
        assertEquals("electromart", config.get("topic.prefix").asText());
        assertEquals("unwrap", config.get("transforms").asText());
    }

    @Test
    void buildConnectorRegistrationWrapsNameAndConfig() {
        ObjectNode registration = PostgresConnector.buildConnectorRegistration("postgres-connector");

        assertEquals("postgres-connector", registration.get("name").asText());
        assertTrue(registration.has("config"));
        assertEquals("sales_slot", registration.get("config").get("slot.name").asText());
    }
}
