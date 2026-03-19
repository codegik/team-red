package com.electromart;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class SalesConsumer {

    private static final AtomicLong duplicatesSkipped = new AtomicLong(0);
    private static final AtomicLong timestampFallbacks = new AtomicLong(0);

    private static LongHistogram pipelineLatency;
    private static LongCounter duplicatesCounter;
    private static LongCounter timestampFallbackCounter;
    private static DoubleCounter revenueTotalCounter;

    public static void main(String[] args) {
        String broker = Env.get("KAFKA_BROKER", "kafka:9092");
        String topic = Env.get("KAFKA_TOPIC_SALES", "sales");
        String groupId = Env.get("KAFKA_GROUP_ID", "timescale-sales-writer");
        int httpPort = Integer.parseInt(Env.get("PORT", "8090"));
        int healthPort = Integer.parseInt(Env.get("HEALTH_PORT", "8086"));

        DatabaseConfig databaseConfig = DatabaseConfig.fromEnv();
        Connection conn = Database.waitForDatabase(databaseConfig);

        initializeMetrics();

        HttpApiServer httpApiServer = new HttpApiServer(databaseConfig, httpPort);
        httpApiServer.start();

        HttpServer healthServer = startHealthServer(healthPort);

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
            System.out.println("Shutting down consumer and HTTP APIs...");
            httpApiServer.stop();
            if (healthServer != null) {
                healthServer.stop(0);
            }
            consumer.wakeup();
        }));

        consumer.subscribe(List.of(topic));
        System.out.printf(
            "Sales Consumer started | topic: %s | group: %s | db: %s | api-port: %d | health-port: %d%n",
            topic, groupId, databaseConfig.jdbcUrl(), httpPort, healthPort
        );

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    if (record.value() == null) {
                        continue;
                    }

                    try {
                        recordInsert(SalesWriter.insertSale(conn, record.value()));
                    } catch (Exception e) {
                        if (!Database.isValid(conn)) {
                            System.err.println("Connection lost, reconnecting...");
                            conn = Database.waitForDatabase(databaseConfig);
                        }

                        try {
                            recordInsert(SalesWriter.insertSale(conn, record.value()));
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
            if (healthServer != null) {
                healthServer.stop(0);
            }
            System.out.println("Consumer stopped");
        }
    }

    private static void initializeMetrics() {
        Meter meter = GlobalOpenTelemetry.getMeter("sales-consumer");
        pipelineLatency = meter
            .histogramBuilder("pipeline.processing.duration")
            .setDescription("End-to-end latency from connector pickup to TimescaleDB insert")
            .setUnit("ms")
            .ofLongs()
            .build();
        duplicatesCounter = meter
            .counterBuilder("pipeline.duplicate.inserts")
            .setDescription("Records skipped due to ON CONFLICT (duplicate sale_id + sale_timestamp)")
            .build();
        timestampFallbackCounter = meter
            .counterBuilder("pipeline.timestamp.fallbacks")
            .setDescription("Records where sale_timestamp failed to parse and fell back to current time")
            .build();
        revenueTotalCounter = meter
            .counterBuilder("sales.revenue.total")
            .ofDoubles()
            .setDescription("Total sale amount successfully inserted, by source")
            .setUnit("BRL")
            .build();
    }

    private static void recordInsert(SalesWriteResult result) {
        if (result.timestampFallbackUsed()) {
            timestampFallbacks.incrementAndGet();
            timestampFallbackCounter.add(1);
        }

        if (!result.inserted()) {
            duplicatesSkipped.incrementAndGet();
            duplicatesCounter.add(1);
            return;
        }

        String source = result.source() == null ? "unknown" : result.source();
        String saleId = result.saleId() == null ? "unknown" : result.saleId();

        Span.current().setAttribute(AttributeKey.stringKey("sale.id"), saleId);
        Span.current().setAttribute(AttributeKey.stringKey("sale.source"), source);

        revenueTotalCounter.add(result.totalAmount(), Attributes.of(AttributeKey.stringKey("source"), source));

        if (result.pickedUpAt() == null || result.pickedUpAt().isBlank()) {
            return;
        }

        try {
            long latencyMs = Instant.now().toEpochMilli() - Instant.parse(result.pickedUpAt()).toEpochMilli();
            pipelineLatency.record(latencyMs, Attributes.of(
                AttributeKey.stringKey("source"), source
            ));
        } catch (Exception ignored) {
        }
    }

    private static HttpServer startHealthServer(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/health", exchange -> {
                String response = String.format(
                    "{\"status\":\"healthy\",\"duplicates_skipped\":%d,\"timestamp_fallbacks\":%d}",
                    duplicatesSkipped.get(),
                    timestampFallbacks.get()
                );
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            System.out.printf("Consumer health endpoint listening on port %d%n", port);
            return server;
        } catch (Exception e) {
            System.err.printf("Failed to start health server on port %d: %s%n", port, e.getMessage());
            return null;
        }
    }
}
