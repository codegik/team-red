package com.electrored.datapipeline.datasource.csv;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class CsvDataGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CsvDataGenerator.class);
    private final Random random = new Random();

    @Value("${csv.output.directory:/data/csv-inbox}")
    private String outputDirectory;

    @Value("${csv.records.per.file:50}")
    private int recordsPerFile;

    private static final String[] PRODUCTS = {
        "iPhone 15 Pro", "Galaxy S24", "MacBook Pro", "Dell XPS",
        "iPad Pro", "Surface Pro", "AirPods Pro", "Sony WH-1000XM5"
    };

    private static final String[] PRODUCT_CODES = {
        "IPHONE15PRO", "GALAXYS24", "MACBOOKPRO", "DELLXPS",
        "IPADPRO", "SURFACEPRO", "AIRPODSPRO", "SONYXM5"
    };

    private static final String[] SELLER_CODES = {
        "SEL011", "SEL012", "SEL013", "SEL014",
        "SEL015", "SEL016", "SEL017", "SEL018"
    };

    private static final String[][] CITIES_COUNTRIES = {
        {"Curitiba", "Brazil"}, {"Recife", "Brazil"}, {"Fortaleza", "Brazil"},
        {"Manaus", "Brazil"}, {"Coimbra", "Portugal"}, {"Braga", "Portugal"},
        {"Valencia", "Spain"}, {"Seville", "Spain"}, {"Rosario", "Argentina"}
    };

    private static final Double[] PRICES = {
        8999.00, 7999.00, 18999.00, 12999.00,
        10999.00, 11999.00, 1899.00, 2299.00
    };

    @Scheduled(fixedDelay = 10000) // Generate every 10 seconds
    public void generateCsvFile() {
        try {
            // Ensure directory exists
            Path dirPath = Paths.get(outputDirectory);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("sales_%s.csv", timestamp);
            Path filePath = dirPath.resolve(filename);

            // Generate CSV data
            try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
                // Write header
                String[] header = {"sale_id", "product_code", "product_name", "seller_code",
                                 "city", "country", "quantity", "amount", "sale_timestamp"};
                writer.writeNext(header);

                // Write records
                for (int i = 0; i < recordsPerFile; i++) {
                    String[] record = generateRecord(timestamp, i);
                    writer.writeNext(record);
                }
            }

            logger.info("Generated CSV file: {} with {} records", filename, recordsPerFile);
        } catch (IOException e) {
            logger.error("Error generating CSV file", e);
        }
    }

    private String[] generateRecord(String timestamp, int index) {
        int productIdx = random.nextInt(PRODUCTS.length);
        String[] cityCountry = CITIES_COUNTRIES[random.nextInt(CITIES_COUNTRIES.length)];
        int quantity = random.nextInt(3) + 1;
        double amount = PRICES[productIdx] * quantity;

        String saleId = String.format("CSV_%s_%04d", timestamp, index);
        String saleTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return new String[]{
            saleId,
            PRODUCT_CODES[productIdx],
            PRODUCTS[productIdx],
            SELLER_CODES[random.nextInt(SELLER_CODES.length)],
            cityCountry[0],
            cityCountry[1],
            String.valueOf(quantity),
            String.format("%.2f", amount),
            saleTimestamp
        };
    }
}

