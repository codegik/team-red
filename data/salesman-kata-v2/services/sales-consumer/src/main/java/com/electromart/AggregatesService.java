package com.electromart;

import java.sql.Connection;
import java.sql.SQLException;

public interface AggregatesService {
    void ping(Connection conn) throws SQLException;
    AggregateResult queryTopCities(Connection conn, String from, String to, int limit) throws SQLException;
    AggregateResult queryTopSalesmen(Connection conn, String from, String to, int limit) throws SQLException;
}
