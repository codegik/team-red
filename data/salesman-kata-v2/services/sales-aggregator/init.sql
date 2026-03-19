CREATE TABLE IF NOT EXISTS sales (
    sale_id TEXT NOT NULL,
    source TEXT NOT NULL,
    product_code TEXT,
    product_name TEXT,
    category TEXT,
    brand TEXT,
    salesman_name TEXT,
    salesman_email TEXT,
    region TEXT,
    store_name TEXT,
    city TEXT,
    store_type TEXT,
    quantity INTEGER,
    unit_price DOUBLE PRECISION,
    total_amount DOUBLE PRECISION,
    sale_timestamp TIMESTAMPTZ NOT NULL,
    trace_id TEXT
);

SELECT create_hypertable('sales', 'sale_timestamp');

CREATE UNIQUE INDEX ON sales (sale_id, sale_timestamp);

CREATE MATERIALIZED VIEW top_products
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', sale_timestamp) AS bucket,
    product_name,
    product_code,
    category,
    brand,
    SUM(total_amount) AS total_revenue,
    SUM(quantity) AS total_quantity,
    COUNT(*) AS total_sales
FROM sales
GROUP BY bucket, product_name, product_code, category, brand;

CREATE MATERIALIZED VIEW top_cities
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', sale_timestamp) AS bucket,
    city,
    region,
    SUM(total_amount) AS total_revenue,
    SUM(quantity) AS total_quantity,
    COUNT(*) AS total_sales
FROM sales
GROUP BY bucket, city, region;

CREATE MATERIALIZED VIEW top_salesmen
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', sale_timestamp) AS bucket,
    salesman_name,
    salesman_email,
    region,
    SUM(total_amount) AS total_revenue,
    SUM(quantity) AS total_quantity,
    COUNT(*) AS total_sales
FROM sales
GROUP BY bucket, salesman_name, salesman_email, region;

SELECT add_continuous_aggregate_policy('top_products',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '5 minutes');

SELECT add_continuous_aggregate_policy('top_cities',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '5 minutes');

SELECT add_continuous_aggregate_policy('top_salesmen',
    start_offset => INTERVAL '3 hours',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '5 minutes');

