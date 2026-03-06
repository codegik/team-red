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
    status TEXT,
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
WHERE status != 'CANCELLED'
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
WHERE status != 'CANCELLED'
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
WHERE status != 'CANCELLED'
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

-- Data Lineage Table for Pipeline Observability
CREATE TABLE IF NOT EXISTS lineage (
    id BIGSERIAL,
    trace_id TEXT NOT NULL,
    sale_id TEXT,
    stage TEXT NOT NULL,
    component TEXT NOT NULL,
    event_type TEXT NOT NULL,
    source_topic TEXT,
    target_topic TEXT,
    latency_ms BIGINT DEFAULT 0,
    metadata JSONB,
    event_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

SELECT create_hypertable('lineage', 'event_timestamp');

CREATE INDEX ON lineage (trace_id, event_timestamp);
CREATE INDEX ON lineage (sale_id, event_timestamp);
CREATE INDEX ON lineage (component, event_timestamp);
CREATE INDEX ON lineage (stage, event_timestamp);

-- Lineage summary view: track data flow
CREATE MATERIALIZED VIEW lineage_flow
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', event_timestamp) AS bucket,
    component,
    stage,
    event_type,
    COUNT(*) AS event_count,
    AVG(latency_ms) AS avg_latency_ms,
    MAX(latency_ms) AS max_latency_ms
FROM lineage
GROUP BY bucket, component, stage, event_type;

SELECT add_continuous_aggregate_policy('lineage_flow',
    start_offset => INTERVAL '1 hour',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute');

-- Pipeline throughput view
CREATE MATERIALIZED VIEW pipeline_throughput
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', event_timestamp) AS bucket,
    stage,
    COUNT(DISTINCT trace_id) AS records_processed
FROM lineage
WHERE event_type = 'published'
GROUP BY bucket, stage;

SELECT add_continuous_aggregate_policy('pipeline_throughput',
    start_offset => INTERVAL '1 hour',
    end_offset => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute');
