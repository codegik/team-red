package com.teamred.datapipeline.fileingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opencsv.CSVReader;
import com.teamred.datapipeline.lineage.LineageContext;
import com.teamred.datapipeline.model.SalesEventDto;
import com.teamred.datapipeline.observability.MetricsRegistry;
import com.teamred.datapipeline.serdes.JsonSerializer;
import io.micrometer.core.instrument.Counter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

public class FileIngestionApplication {

    private static final Logger logger = LoggerFactory.getLogger(FileIngestionApplication.class);
    private static final String TOPIC = "sales.raw.file";
    private static Counter recordsProcessed;
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public static void main(String[] args) throws Exception {
        recordsProcessed = MetricsRegistry.counter("file_ingestion_records_processed");

        String kafkaBootstrapServers = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
        String watchDirectory = System.getenv().getOrDefault("WATCH_DIRECTORY", "./data/input");
        String archiveDirectory = System.getenv().getOrDefault("ARCHIVE_DIRECTORY", "./data/archive");

        Properties kafkaProps = new Properties();
        kafkaProps.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        kafkaProps.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProps.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        kafkaProps.setProperty(ProducerConfig.ACKS_CONFIG, "all");
        kafkaProps.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        KafkaProducer<String, SalesEventDto> producer = new KafkaProducer<>(kafkaProps);

        Path watchPath = Paths.get(watchDirectory);
        Path archivePath = Paths.get(archiveDirectory);
        Files.createDirectories(watchPath);
        Files.createDirectories(archivePath);

        WatchService watchService = FileSystems.getDefault().newWatchService();
        watchPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

        logger.info("File Ingestion Service started, watching: {}", watchDirectory);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down File Ingestion Service");
            producer.close();
        }));

        while (true) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();
                Path fullPath = watchPath.resolve(filename);

                if (Files.isRegularFile(fullPath)) {
                    Thread.sleep(100);
                    processFile(fullPath, producer, archivePath);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private static void processFile(Path filePath, KafkaProducer<String, SalesEventDto> producer, Path archivePath) {
        logger.info("Processing file: {}", filePath);

        try {
            String fileName = filePath.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".csv")) {
                processCsvFile(filePath, producer);
            } else if (fileName.endsWith(".json")) {
                processJsonFile(filePath, producer);
            } else {
                logger.warn("Unknown file type: {}", fileName);
                return;
            }

            Path target = archivePath.resolve(filePath.getFileName());
            Files.move(filePath, target, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Archived file: {}", target);

        } catch (Exception e) {
            logger.error("Error processing file: {}", filePath, e);
        }
    }

    private static void processCsvFile(Path filePath, KafkaProducer<String, SalesEventDto> producer) throws Exception {
        try (CSVReader reader = new CSVReader(new FileReader(filePath.toFile()))) {
            String[] header = reader.readNext();
            String[] line;

            while ((line = reader.readNext()) != null) {
                SalesEventDto event = parseCsvLine(header, line);
                sendEvent(event, producer);
            }
        }
    }

    private static SalesEventDto parseCsvLine(String[] header, String[] line) {
        SalesEventDto event = new SalesEventDto();
        for (int i = 0; i < header.length; i++) {
            String key = header[i].trim();
            String value = line[i].trim();

            switch (key) {
                case "sale_id" -> event.setSaleId(value);
                case "timestamp" -> event.setTimestamp(Long.parseLong(value));
                case "salesman_id" -> event.setSalesmanId(value);
                case "salesman_name" -> event.setSalesmanName(value);
                case "customer_id" -> event.setCustomerId(value);
                case "product_id" -> event.setProductId(value);
                case "product_name" -> event.setProductName(value);
                case "quantity" -> event.setQuantity(Integer.parseInt(value));
                case "unit_price" -> event.setUnitPrice(Double.parseDouble(value));
                case "total_amount" -> event.setTotalAmount(Double.parseDouble(value));
                case "city" -> event.setCity(value);
                case "country" -> event.setCountry(value);
            }
        }

        event.setSourceSystem("FILE");
        event.setIngestionTimestamp(System.currentTimeMillis());
        event.setLineageId(LineageContext.generateLineageId());

        return event;
    }

    private static void processJsonFile(Path filePath, KafkaProducer<String, SalesEventDto> producer) throws IOException {
        SalesEventDto[] events = objectMapper.readValue(filePath.toFile(), SalesEventDto[].class);
        for (SalesEventDto event : events) {
            event.setSourceSystem("FILE");
            event.setIngestionTimestamp(System.currentTimeMillis());
            event.setLineageId(LineageContext.generateLineageId());
            sendEvent(event, producer);
        }
    }

    private static void sendEvent(SalesEventDto event, KafkaProducer<String, SalesEventDto> producer) {
        RecordHeaders headers = new RecordHeaders();
        LineageContext.addLineageHeaders(headers, event.getLineageId(), "FILE", event.getTimestamp());

        ProducerRecord<String, SalesEventDto> record = new ProducerRecord<>(
                TOPIC,
                null,
                event.getSaleId(),
                event,
                headers
        );

        producer.send(record);
        recordsProcessed.increment();
        logger.info("Sent sale event: {}", event.getSaleId());
    }
}
