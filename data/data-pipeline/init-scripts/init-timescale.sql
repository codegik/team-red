CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS top_sales_by_city (
    id SERIAL,
    city VARCHAR(100) NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    total_sales DECIMAL(15, 2) NOT NULL,
    transaction_count INTEGER NOT NULL,
    top_product VARCHAR(255),
    top_product_sales DECIMAL(15, 2),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (city, window_start)
);

SELECT create_hypertable('top_sales_by_city', 'window_start', if_not_exists => TRUE);

CREATE INDEX idx_city_sales_city ON top_sales_by_city(city, window_start DESC);
CREATE INDEX idx_city_sales_window ON top_sales_by_city(window_start DESC);

CREATE TABLE IF NOT EXISTS top_salesman_country (
    id SERIAL,
    salesman_id VARCHAR(100) NOT NULL,
    salesman_name VARCHAR(255) NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    total_sales DECIMAL(15, 2) NOT NULL,
    transaction_count INTEGER NOT NULL,
    cities_covered INTEGER NOT NULL,
    rank INTEGER,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (salesman_id, window_start)
);

SELECT create_hypertable('top_salesman_country', 'window_start', if_not_exists => TRUE);

CREATE INDEX idx_salesman_sales ON top_salesman_country(salesman_id, window_start DESC);
CREATE INDEX idx_salesman_window ON top_salesman_country(window_start DESC);
CREATE INDEX idx_salesman_rank ON top_salesman_country(window_start DESC, rank);

CREATE TABLE IF NOT EXISTS data_lineage (
    lineage_id VARCHAR(100) NOT NULL,
    sale_id VARCHAR(100) NOT NULL,
    source_system VARCHAR(20) NOT NULL,
    source_timestamp TIMESTAMPTZ NOT NULL,
    ingestion_timestamp TIMESTAMPTZ NOT NULL,
    kafka_topic VARCHAR(255),
    kafka_partition INTEGER,
    kafka_offset BIGINT,
    transformation_steps JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (lineage_id, source_timestamp)
);

SELECT create_hypertable('data_lineage', 'source_timestamp', if_not_exists => TRUE);

CREATE INDEX idx_lineage_sale_id ON data_lineage(sale_id, source_timestamp DESC);
CREATE INDEX idx_lineage_source ON data_lineage(source_system, source_timestamp DESC);
CREATE INDEX idx_lineage_id ON data_lineage(lineage_id);

CREATE MATERIALIZED VIEW IF NOT EXISTS hourly_city_sales_summary
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', window_start) AS bucket,
    city,
    SUM(total_sales) AS total_sales,
    SUM(transaction_count) AS total_transactions,
    AVG(total_sales) AS avg_sales_per_window
FROM top_sales_by_city
GROUP BY bucket, city
WITH NO DATA;

SELECT add_continuous_aggregate_policy('hourly_city_sales_summary',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour',
    if_not_exists => TRUE);

CREATE MATERIALIZED VIEW IF NOT EXISTS daily_salesman_performance
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', window_start) AS bucket,
    salesman_id,
    salesman_name,
    SUM(total_sales) AS total_sales,
    SUM(transaction_count) AS total_transactions,
    AVG(total_sales) AS avg_sales_per_window,
    MAX(cities_covered) AS max_cities_covered
FROM top_salesman_country
GROUP BY bucket, salesman_id, salesman_name
WITH NO DATA;

SELECT add_continuous_aggregate_policy('daily_salesman_performance',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day',
    if_not_exists => TRUE);
