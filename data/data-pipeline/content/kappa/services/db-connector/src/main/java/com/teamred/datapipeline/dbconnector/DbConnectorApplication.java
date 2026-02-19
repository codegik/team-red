package com.teamred.datapipeline.dbconnector;

import com.teamred.datapipeline.lineage.LineageContext;
import com.teamred.datapipeline.model.SalesEventDto;
import com.teamred.datapipeline.observability.MetricsRegistry;
import com.teamred.datapipeline.serdes.JsonSerializer;
import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import io.micrometer.core.instrument.Counter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DbConnectorApplication {

    private static final Logger logger = LoggerFactory.getLogger(DbConnectorApplication.class);
    private static final String TOPIC = "sales.raw.db";
    private static Counter recordsProcessed;

    public static void main(String[] args) {
        recordsProcessed = MetricsRegistry.counter("db_connector_records_processed");

        String kafkaBootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String dbHost = System.getenv().getOrDefault("DATABASE_HOSTNAME", "localhost");
        String dbPort = System.getenv().getOrDefault("DATABASE_PORT", "5432");
        String dbUser = System.getenv().getOrDefault("DATABASE_USER", "sourceuser");
        String dbPassword = System.getenv().getOrDefault("DATABASE_PASSWORD", "sourcepass");
        String dbName = System.getenv().getOrDefault("DATABASE_DBNAME", "sourcedb");

        Properties kafkaProps = new Properties();
        kafkaProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        kafkaProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        kafkaProps.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        kafkaProps.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        KafkaProducer<String, SalesEventDto> producer = new KafkaProducer<>(kafkaProps);

        Configuration config = Configuration.create()
                .with("name", "postgres-connector")
                .with("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", "/tmp/offsets.dat")
                .with("offset.flush.interval.ms", "1000")
                .with("database.hostname", dbHost)
                .with("database.port", dbPort)
                .with("database.user", dbUser)
                .with("database.password", dbPassword)
                .with("database.dbname", dbName)
                .with("database.server.name", "sourcedb")
                .with("table.include.list", "public.sales")
                .with("plugin.name", "pgoutput")
                .with("publication.autocreate.mode", "filtered")
                .with("slot.name", "debezium_slot")
                .with("topic.prefix", "sourcedb")
                .build();

        try (DebeziumEngine<RecordChangeEvent<SourceRecord>> engine = DebeziumEngine.create(ChangeEventFormat.of(Connect.class))
                .using(config.asProperties())
                .notifying(record -> processRecord(record, producer))
                .build()) {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(engine);

            logger.info("DB Connector started");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down DB Connector");
                try {
                    engine.close();
                    producer.close();
                } catch (Exception e) {
                    logger.error("Error during shutdown", e);
                }
            }));

            executor.shutdown();
            while (!executor.isTerminated()) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            logger.error("Error running DB Connector", e);
        }
    }

    private static void processRecord(RecordChangeEvent<SourceRecord> record, KafkaProducer<String, SalesEventDto> producer) {
        SourceRecord sourceRecord = record.record();
        if (sourceRecord.value() == null) {
            return;
        }

        try {
            Struct value = (Struct) sourceRecord.value();
            Struct after = value.getStruct("after");

            if (after != null) {
                SalesEventDto event = new SalesEventDto();
                event.setSaleId(after.getString("sale_id"));
                event.setTimestamp(after.getInt64("timestamp"));
                event.setSalesmanId(after.getString("salesman_id"));
                event.setSalesmanName(after.getString("salesman_name"));
                event.setCustomerId(after.getString("customer_id"));
                event.setProductId(after.getString("product_id"));
                event.setProductName(after.getString("product_name"));
                event.setQuantity(after.getInt32("quantity"));
                Object unitPrice = after.get("unit_price");
                event.setUnitPrice(unitPrice instanceof Number ? ((Number) unitPrice).doubleValue() : 0.0);
                Object totalAmount = after.get("total_amount");
                event.setTotalAmount(totalAmount instanceof Number ? ((Number) totalAmount).doubleValue() : 0.0);
                event.setCity(after.getString("city"));
                event.setCountry(after.getString("country"));
                event.setSourceSystem("DB");
                event.setIngestionTimestamp(System.currentTimeMillis());

                String lineageId = LineageContext.generateLineageId();
                event.setLineageId(lineageId);

                RecordHeaders headers = new RecordHeaders();
                LineageContext.addLineageHeaders(headers, lineageId, "DB", event.getTimestamp());

                ProducerRecord<String, SalesEventDto> producerRecord = new ProducerRecord<>(
                        TOPIC,
                        null,
                        event.getSaleId(),
                        event,
                        headers
                );

                producer.send(producerRecord);
                recordsProcessed.increment();
                logger.info("Sent sale event: {}", event.getSaleId());
            }
        } catch (Exception e) {
            logger.error("Error processing record", e);
        }
    }
}
