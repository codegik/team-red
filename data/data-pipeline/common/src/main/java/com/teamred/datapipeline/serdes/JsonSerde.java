package com.teamred.datapipeline.serdes;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerde<T> implements Serde<T> {

    private final JsonSerializer<T> serializer;
    private final JsonDeserializer<T> deserializer;

    public JsonSerde(Class<T> targetType) {
        this.serializer = new JsonSerializer<>();
        this.deserializer = new JsonDeserializer<>(targetType);
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }
}
