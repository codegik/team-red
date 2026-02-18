package com.teamred.datapipeline.processor.sink;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

public class TimescaleSink {

    private static final Logger logger = LoggerFactory.getLogger(TimescaleSink.class);
    private final HikariDataSource dataSource;

    public TimescaleSink(String host, int port, String database, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        this.dataSource = new HikariDataSource(config);
    }

    public void insertCitySales(String city, long windowStart, long windowEnd, double totalSales,
                                 int transactionCount, String topProduct, double topProductSales) {
        String sql = """
                INSERT INTO top_sales_by_city (city, window_start, window_end, total_sales, transaction_count, top_product, top_product_sales)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (city, window_start) DO UPDATE SET
                    window_end = EXCLUDED.window_end,
                    total_sales = EXCLUDED.total_sales,
                    transaction_count = EXCLUDED.transaction_count,
                    top_product = EXCLUDED.top_product,
                    top_product_sales = EXCLUDED.top_product_sales
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, city);
            stmt.setTimestamp(2, new Timestamp(windowStart));
            stmt.setTimestamp(3, new Timestamp(windowEnd));
            stmt.setDouble(4, totalSales);
            stmt.setInt(5, transactionCount);
            stmt.setString(6, topProduct);
            stmt.setDouble(7, topProductSales);

            stmt.executeUpdate();
            logger.debug("Inserted city sales: {} - ${}", city, totalSales);

        } catch (Exception e) {
            logger.error("Error inserting city sales", e);
        }
    }

    public void insertSalesmanStats(String salesmanId, String salesmanName, long windowStart, long windowEnd,
                                     double totalSales, int transactionCount, int citiesCovered) {
        String sql = """
                INSERT INTO top_salesman_country (salesman_id, salesman_name, window_start, window_end, total_sales, transaction_count, cities_covered)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (salesman_id, window_start) DO UPDATE SET
                    salesman_name = EXCLUDED.salesman_name,
                    window_end = EXCLUDED.window_end,
                    total_sales = EXCLUDED.total_sales,
                    transaction_count = EXCLUDED.transaction_count,
                    cities_covered = EXCLUDED.cities_covered
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, salesmanId);
            stmt.setString(2, salesmanName);
            stmt.setTimestamp(3, new Timestamp(windowStart));
            stmt.setTimestamp(4, new Timestamp(windowEnd));
            stmt.setDouble(5, totalSales);
            stmt.setInt(6, transactionCount);
            stmt.setInt(7, citiesCovered);

            stmt.executeUpdate();
            logger.debug("Inserted salesman stats: {} - ${}", salesmanName, totalSales);

        } catch (Exception e) {
            logger.error("Error inserting salesman stats", e);
        }
    }

    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
