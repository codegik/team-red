package com.electromart;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.Connection;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class SalesConsumer {

    public static void main(String[] args) {
        String broker = Env.get("KAFKA_BROKER", "kafka:9092");
        String topic = Env.get("KAFKA_TOPIC_SALES", "sales");
        String groupId = Env.get("KAFKA_GROUP_ID", "timescale-sales-writer");
        String httpPort = Env.get("PORT", "8090");

        DatabaseConfig databaseConfig = DatabaseConfig.fromEnv();
        Connection conn = Database.waitForDatabase(databaseConfig);

        HttpApiServer httpApiServer = new HttpApiServer(databaseConfig, Integer.parseInt(httpPort));
        httpApiServer.start();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down consumer and HTTP API...");
            httpApiServer.stop();
            consumer.wakeup();
        }));

        consumer.subscribe(List.of(topic));
        System.out.printf(
            "Sales Consumer started | topic: %s | group: %s | db: %s | port: %s%n",
            topic, groupId, databaseConfig.jdbcUrl(), httpPort
        );

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    if (record.value() == null) {
                        continue;
                    }

                    try {
                        SalesWriter.insertSale(conn, record.value());
                    } catch (Exception e) {
                        if (!Database.isValid(conn)) {
                            System.err.println("Connection lost, reconnecting...");
                            conn = Database.waitForDatabase(databaseConfig);
                        }

                        try {
                            SalesWriter.insertSale(conn, record.value());
                        } catch (Exception retryEx) {
                            System.err.printf(
                                "Failed to insert sale from partition %d offset %d: %s%n",
                                record.partition(), record.offset(), retryEx.getMessage()
                            );
                        }
                    }
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            System.err.println("Consumer error: " + e.getMessage());
        } finally {
            consumer.close();
            Database.closeQuietly(conn);
            httpApiServer.stop();
            System.out.println("Consumer stopped");
        }
    }
}
