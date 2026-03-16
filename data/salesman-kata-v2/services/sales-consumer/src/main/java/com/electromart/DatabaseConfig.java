package com.electromart;

public record DatabaseConfig(
    String host,
    String port,
    String user,
    String password,
    String database
) {

    public static DatabaseConfig fromEnv() {
        return new DatabaseConfig(
            Env.get("TIMESCALEDB_HOST", "timescaledb"),
            Env.get("TIMESCALEDB_PORT", "5432"),
            Env.get("TIMESCALEDB_USER", "sales"),
            Env.get("TIMESCALEDB_PASSWORD", "sales123"),
            Env.get("TIMESCALEDB_DATABASE", "salesdb")
        );
    }

    public String jdbcUrl() {
        return String.format("jdbc:postgresql://%s:%s/%s", host, port, database);
    }
}
