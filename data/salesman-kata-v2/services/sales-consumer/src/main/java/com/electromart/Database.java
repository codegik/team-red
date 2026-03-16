package com.electromart;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {

    private Database() {
    }

    public static Connection waitForDatabase(DatabaseConfig config) {
        while (true) {
            try {
                Connection conn = DriverManager.getConnection(config.jdbcUrl(), config.user(), config.password());
                conn.setAutoCommit(true);
                System.out.println("Connected to TimescaleDB");
                return conn;
            } catch (SQLException e) {
                System.out.printf("Waiting for TimescaleDB at %s ...%n", config.jdbcUrl());
                sleep(3000);
            }
        }
    }

    public static Connection connect(DatabaseConfig config) throws SQLException {
        Connection conn = DriverManager.getConnection(config.jdbcUrl(), config.user(), config.password());
        conn.setAutoCommit(true);
        return conn;
    }

    public static boolean isValid(Connection conn) {
        if (conn == null) {
            return false;
        }

        try {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public static void closeQuietly(Connection conn) {
        if (conn == null) {
            return;
        }

        try {
            conn.close();
        } catch (Exception ignored) {
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
