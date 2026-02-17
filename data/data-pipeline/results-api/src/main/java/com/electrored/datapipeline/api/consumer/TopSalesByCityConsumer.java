package com.electrored.datapipeline.api.consumer;

import com.electrored.datapipeline.api.model.TopSalesByCity;
import com.electrored.datapipeline.api.repository.TopSalesByCityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TopSalesByCityConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TopSalesByCityConsumer.class);
    private final TopSalesByCityRepository repository;
    private final ObjectMapper objectMapper;

    public TopSalesByCityConsumer(TopSalesByCityRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "top-sales-by-city", groupId = "results-api-group")
    public void consume(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            TopSalesByCity entity = new TopSalesByCity();
            entity.setCity(jsonNode.get("city").asText());
            entity.setCountry(jsonNode.get("country").asText());
            entity.setTotalSales(jsonNode.get("totalSales").asDouble());
            entity.setTransactionCount(jsonNode.get("transactionCount").asLong());
            entity.setWindowStart(jsonNode.get("windowStart").asLong());
            entity.setWindowEnd(jsonNode.get("windowEnd").asLong());
            entity.setUpdatedAt(LocalDateTime.now());

            repository.save(entity);
            logger.info("Saved top sales for city: {} with total: {}",
                entity.getCity(), entity.getTotalSales());
        } catch (Exception e) {
            logger.error("Error consuming top sales by city message", e);
        }
    }
}

