package com.electrored.datapipeline.datasource.postgres;

import com.electrored.datapipeline.datasource.kafka.SaleEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PostgresDataGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PostgresDataGenerator.class);
    private final SaleRepository saleRepository;
    private final SaleEventProducer saleEventProducer;
    private final Random random = new Random();

    private static final String[] PRODUCTS = {
        "iPhone 15 Pro", "Galaxy S24", "MacBook Pro", "Dell XPS",
        "iPad Pro", "Surface Pro", "AirPods Pro", "Sony WH-1000XM5"
    };

    private static final String[] PRODUCT_CODES = {
        "IPHONE15PRO", "GALAXYS24", "MACBOOKPRO", "DELLXPS",
        "IPADPRO", "SURFACEPRO", "AIRPODSPRO", "SONYXM5"
    };

    private static final String[] SELLERS = {
        "João Silva", "Maria Santos", "Pedro Costa", "Ana Oliveira",
        "Lucas Ferreira", "Juliana Lima", "Rafael Almeida", "Camila Rodrigues"
    };

    private static final String[] SELLER_CODES = {
        "SEL001", "SEL002", "SEL003", "SEL004",
        "SEL005", "SEL006", "SEL007", "SEL008"
    };

    private static final String[][] CITIES_COUNTRIES = {
        {"São Paulo", "Brazil"}, {"Rio de Janeiro", "Brazil"}, {"Belo Horizonte", "Brazil"},
        {"Brasília", "Brazil"}, {"Porto Alegre", "Brazil"}, {"Salvador", "Brazil"},
        {"Lisbon", "Portugal"}, {"Porto", "Portugal"}, {"Madrid", "Spain"},
        {"Barcelona", "Spain"}, {"Buenos Aires", "Argentina"}, {"Córdoba", "Argentina"}
    };

    private static final Double[] PRICES = {
        8999.00, 7999.00, 18999.00, 12999.00,
        10999.00, 11999.00, 1899.00, 2299.00
    };

    public PostgresDataGenerator(SaleRepository saleRepository, SaleEventProducer saleEventProducer) {
        this.saleRepository = saleRepository;
        this.saleEventProducer = saleEventProducer;
    }

    @Scheduled(fixedDelay = 5000) // Generate every 5 seconds
    public void generateSales() {
        try {
            int numSales = random.nextInt(5) + 1; // 1-5 sales per batch

            for (int i = 0; i < numSales; i++) {
                Sale sale = new Sale();

                int productIdx = random.nextInt(PRODUCTS.length);
                sale.setProductCode(PRODUCT_CODES[productIdx]);
                sale.setProductName(PRODUCTS[productIdx]);

                int sellerIdx = random.nextInt(SELLERS.length);
                sale.setSellerCode(SELLER_CODES[sellerIdx]);
                sale.setSellerName(SELLERS[sellerIdx]);

                String[] cityCountry = CITIES_COUNTRIES[random.nextInt(CITIES_COUNTRIES.length)];
                sale.setCity(cityCountry[0]);
                sale.setCountry(cityCountry[1]);

                int quantity = random.nextInt(3) + 1;
                sale.setQuantity(quantity);
                sale.setAmount(PRICES[productIdx] * quantity);

                sale.setSaleTimestamp(LocalDateTime.now());
                sale.setStatus("PENDING");

                Sale savedSale = saleRepository.save(sale);

                // Publish to Kafka
                saleEventProducer.sendSaleEvent(savedSale);
            }

            logger.info("Generated {} sales in PostgreSQL and published to Kafka", numSales);
        } catch (Exception e) {
            logger.error("Error generating sales", e);
        }
    }
}

