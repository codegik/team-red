package com.electromart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

public class SchemaValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final String subject;
    private final JsonSchema schema;

    public SchemaValidator(String registryUrl, String subject, int version) throws Exception {
        this.subject = subject;
        String schemaJson = fetchSchema(registryUrl, subject, version);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        this.schema = factory.getSchema(mapper.readTree(schemaJson));
        System.out.printf("[SchemaValidator] Loaded schema for subject '%s' version %d%n", subject, version);
    }

    public static SchemaValidator tryCreate(String registryUrl, String subject, int version) {
        try {
            return new SchemaValidator(registryUrl, subject, version);
        } catch (Exception e) {
            System.err.printf("[SchemaValidator] Could not load schema for '%s' v%d: %s — validation disabled%n",
                subject, version, e.getMessage());
            return null;
        }
    }

    public boolean isValid(JsonNode node) {
        Set<ValidationMessage> errors = schema.validate(node);
        if (!errors.isEmpty()) {
            System.err.printf("[Schema] Invalid record for subject '%s': %s%n", subject, errors);
            return false;
        }
        return true;
    }

    private static String fetchSchema(String registryUrl, String subject, int version) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String url = registryUrl + "/subjects/" + subject + "/versions/" + version;
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Schema Registry returned HTTP " + response.statusCode() + " for " + url);
        }
        // Response: {"subject":"...","version":1,"id":1,"schemaType":"JSON","schema":"<escaped JSON string>"}
        return mapper.readTree(response.body()).get("schema").asText();
    }
}
