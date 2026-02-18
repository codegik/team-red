package com.teamred.datapipeline.lineage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamred.datapipeline.model.SalesEventDto;
import com.teamred.datapipeline.serdes.JsonDeserializer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;

public class LineageTrackerApplication {

    private static final Logger logger = LoggerFactory.getLogger(LineageTrackerApplication.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        String kafkaBootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String timescaleHost = System.getenv().getOrDefault("TIMESCALE_HOST", "localhost");
        int timescalePort = Integer.parseInt(System.getenv().getOrDefault("TIMESCALE_PORT", "5433"));
        String timescaleDatabase = System.getenv().getOrDefault("TIMESCALE_DATABASE", "analyticsdb");
        String timescaleUser = System.getenv().getOrDefault("TIMESCALE_USER", "analyticsuser");
        String timescalePassword = System.getenv().getOrDefault("TIMESCALE_PASSWORD", "analyticspass");

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "lineage-tracker-group");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", timescaleHost, timescalePort, timescaleDatabase));
        config.setUsername(timescaleUser);
        config.setPassword(timescalePassword);
        config.setMaximumPoolSize(5);
        HikariDataSource dataSource = new HikariDataSource(config);

        KafkaConsumer<String, SalesEventDto> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), new JsonDeserializer<>(SalesEventDto.class));
        consumer.subscribe(Arrays.asList("sales.raw.db", "sales.raw.file", "sales.raw.soap"));

        logger.info("Lineage Tracker started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Lineage Tracker");
            consumer.close();
            dataSource.close();
        }));

        while (true) {
            ConsumerRecords<String, SalesEventDto> records = consumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<String, SalesEventDto> record : records) {
                try {
                    SalesEventDto event = record.value();

                    String lineageId = com.teamred.datapipeline.lineage.LineageContext.getHeaderValue(record.headers(), com.teamred.datapipeline.lineage.LineageContext.LINEAGE_ID_HEADER);
                    String sourceSystem = com.teamred.datapipeline.lineage.LineageContext.getHeaderValue(record.headers(), com.teamred.datapipeline.lineage.LineageContext.SOURCE_SYSTEM_HEADER);
                    String sourceTimestamp = com.teamred.datapipeline.lineage.LineageContext.getHeaderValue(record.headers(), com.teamred.datapipeline.lineage.LineageContext.SOURCE_TIMESTAMP_HEADER);
                    String ingestionTimestamp = com.teamred.datapipeline.lineage.LineageContext.getHeaderValue(record.headers(), com.teamred.datapipeline.lineage.LineageContext.INGESTION_TIMESTAMP_HEADER);

                    Map<String, Object> transformationSteps = new HashMap<>();
                    transformationSteps.put("step", "ingestion");
                    transformationSteps.put("topic", record.topic());
                    transformationSteps.put("partition", record.partition());
                    transformationSteps.put("offset", record.offset());

                    String transformationJson = objectMapper.writeValueAsString(transformationSteps);

                    insertLineage(dataSource, lineageId, event.getSaleId(), sourceSystem,
                            Long.parseLong(sourceTimestamp), Long.parseLong(ingestionTimestamp),
                            record.topic(), record.partition(), record.offset(), transformationJson);

                    logger.debug("Tracked lineage for sale: {}", event.getSaleId());

                } catch (Exception e) {
                    logger.error("Error tracking lineage", e);
                }
            }
        }
    }

    private static void insertLineage(HikariDataSource dataSource, String lineageId, String saleId,
                                       String sourceSystem, long sourceTimestamp, long ingestionTimestamp,
                                       String kafkaTopic, int kafkaPartition, long kafkaOffset,
                                       String transformationSteps) {
        String sql = """
                INSERT INTO data_lineage (lineage_id, sale_id, source_system, source_timestamp, ingestion_timestamp,
                                           kafka_topic, kafka_partition, kafka_offset, transformation_steps)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                ON CONFLICT (lineage_id) DO UPDATE SET
                    transformation_steps = data_lineage.transformation_steps || EXCLUDED.transformation_steps
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, lineageId);
            stmt.setString(2, saleId);
            stmt.setString(3, sourceSystem);
            stmt.setTimestamp(4, new Timestamp(sourceTimestamp));
            stmt.setTimestamp(5, new Timestamp(ingestionTimestamp));
            stmt.setString(6, kafkaTopic);
            stmt.setInt(7, kafkaPartition);
            stmt.setLong(8, kafkaOffset);
            stmt.setString(9, transformationSteps);

            stmt.executeUpdate();

        } catch (Exception e) {
            logger.error("Error inserting lineage", e);
        }
    }
}
