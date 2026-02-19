package com.teamred.datapipeline.serdes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonDeserializer<T> implements Deserializer<T> {

    private static final Logger logger = LoggerFactory.getLogger(JsonDeserializer.class);
    private final ObjectMapper objectMapper;
    private final Class<T> targetType;

    public JsonDeserializer(Class<T> targetType) {
        this.targetType = targetType;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            return objectMapper.readValue(data, targetType);
        } catch (Exception e) {
            logger.error("Failed to deserialize data", e);
            throw new RuntimeException("Failed to deserialize data", e);
        }
    }
}
