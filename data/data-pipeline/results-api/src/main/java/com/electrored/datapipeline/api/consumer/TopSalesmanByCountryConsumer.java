package com.electrored.datapipeline.api.consumer;

import com.electrored.datapipeline.api.model.TopSalesmanByCountry;
import com.electrored.datapipeline.api.repository.TopSalesmanByCountryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TopSalesmanByCountryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TopSalesmanByCountryConsumer.class);
    private final TopSalesmanByCountryRepository repository;
    private final ObjectMapper objectMapper;

    public TopSalesmanByCountryConsumer(TopSalesmanByCountryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "top-salesman-by-country", groupId = "results-api-group")
    public void consume(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);

            TopSalesmanByCountry entity = new TopSalesmanByCountry();
            entity.setSellerCode(jsonNode.get("sellerCode").asText());
            entity.setSellerName(jsonNode.get("sellerName").asText());
            entity.setCountry(jsonNode.get("country").asText());
            entity.setTotalSales(jsonNode.get("totalSales").asDouble());
            entity.setTransactionCount(jsonNode.get("transactionCount").asLong());
            entity.setWindowStart(jsonNode.get("windowStart").asLong());
            entity.setWindowEnd(jsonNode.get("windowEnd").asLong());
            entity.setUpdatedAt(LocalDateTime.now());

            repository.save(entity);
            logger.info("Saved top salesman: {} ({}) in {} with total: {}",
                entity.getSellerName(), entity.getSellerCode(),
                entity.getCountry(), entity.getTotalSales());
        } catch (Exception e) {
            logger.error("Error consuming top salesman by country message", e);
        }
    }
}

