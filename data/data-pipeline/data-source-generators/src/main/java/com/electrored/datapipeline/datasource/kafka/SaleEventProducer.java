package com.electrored.datapipeline.datasource.kafka;

import com.electrored.datapipeline.datasource.postgres.Sale;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SaleEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(SaleEventProducer.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.sales:sales-events}")
    private String salesTopic;

    public SaleEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendSaleEvent(Sale sale) {
        try {
            Map<String, Object> saleEvent = new HashMap<>();
            saleEvent.put("saleId", String.valueOf(sale.getSaleId()));
            saleEvent.put("productCode", sale.getProductCode());
            saleEvent.put("productName", sale.getProductName());
            saleEvent.put("sellerCode", sale.getSellerCode());
            saleEvent.put("sellerName", sale.getSellerName());
            saleEvent.put("city", sale.getCity());
            saleEvent.put("country", sale.getCountry());
            saleEvent.put("amount", sale.getAmount());
            saleEvent.put("quantity", sale.getQuantity());
            saleEvent.put("saleTimestamp", System.currentTimeMillis());
            saleEvent.put("source", "POSTGRES");

            String message = objectMapper.writeValueAsString(saleEvent);
            kafkaTemplate.send(salesTopic, String.valueOf(sale.getSaleId()), message);

            logger.debug("Sent sale event to Kafka: {}", sale.getSaleId());
        } catch (Exception e) {
            logger.error("Error sending sale event to Kafka", e);
        }
    }
}

