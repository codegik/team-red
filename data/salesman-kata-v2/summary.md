# Summary

## Data Sources

Data sources represent the three independent origins of sales data in this pipeline. Each source simulates a different real-world integration scenario — a relational database, flat file exports, and a legacy web service — all producing sales transactions from the same product catalog but serving different geographic regions. Data generators continuously create synthetic sales records every 5 seconds, simulating a live retail operation.

### 1) Postgres

PostgreSQL acts as the primary transactional database, representing the original ERP system (SAP on PostgreSQL). It holds real-time point-of-sale transactions for the São Paulo region. A Node.js generator continuously inserts new sales, simulating POS terminal activity.

#### Postgres Database

PostgreSQL 15 (Alpine) with `wal_level=logical` enabled for CDC replication. The database `electromart` contains four tables: `products` (22 electronics items), `salesmen` (5 reps in São Paulo), `stores` (5 retail locations in São Paulo), and `sales` (transactions with status PENDING/CONFIRMED/CANCELLED). Schema is auto-initialized via `init.sql` mounted into the container.

#### Postgres Data Generator

Node.js 20 application using the `pg` client library (v8.13.0). On startup it inserts 10 initial sales, then continuously generates 1–5 random sales every 5 seconds. Each sale randomly picks a product, salesman, and store from the existing tables, applies a status distribution (70% PENDING, 25% CONFIRMED, 5% CANCELLED), and calculates the total from quantity and unit price.

### 2) CSV Files

CSV files represent a legacy data source from an acquired company (2018). Sales are exported as CSV files into S3-compatible object storage, mimicking a batch-export system. This source covers the Minas Gerais region with its own set of salesmen and stores.

#### Minio Bucket

MinIO (`minio/minio:latest`) provides S3-compatible object storage. It exposes the S3 API on port 9000 and a web console on port 9001 (credentials: minioadmin/minioadmin123). Two buckets are configured: `sales-csv` for incoming files and `sales-csv-processed` for archived files. A webhook notification is configured to POST to the CSV connector on `s3:ObjectCreated:Put` events, enabling event-driven processing.

#### CSV Data Generator

Node.js 20 application using the MinIO client library (v8.0.0). It generates a CSV file with 1–5 random sales records every 5 seconds and uploads it to the `sales-csv` bucket. Each file includes a header row and 16 columns (sale_id, product info, salesman info, store info, transaction details). Sale IDs are prefixed with `CSV` followed by a date and random digits.

### 3) Soap Service

The SOAP service represents a legacy sales system (2012) that exposes data via SOAP 1.1/XML. It simulates an older enterprise integration pattern where data must be actively polled. This source covers the Rio de Janeiro region.

#### SOAP Endpoint with data generator

Node.js 20 with Express.js (v4.18.2) running on port 8080. It exposes a POST `/sales` endpoint that accepts SOAP XML requests with cursor-based pagination and returns SOAP 1.1 responses containing 1–5 randomly generated sales per request. The `hasMore` flag is true ~20% of the time to simulate paginated results. A `/health` endpoint provides basic health checks.

## Data Ingestion

Data ingestion is the process of extracting data from the three heterogeneous sources, converting it into JSON, and publishing it to Kafka. Each source has a dedicated connector that handles the specific protocol (CDC, S3 webhooks, or SOAP polling) and publishes to its own dedicated Kafka topic (`raw_csv`, `raw_soap`, `raw_postgres`).
### Connector sources

Source connectors are responsible for reading data from external systems and publishing it into Kafka. Each connector is a standalone Java 17 application that handles connection management, data transformation, and topic auto-creation. Connectors are intentionally simple — they only convert the source format to JSON and publish to their dedicated topic. Normalization (adding `source`, `trace_id`, `ingested_at` metadata) is handled downstream by the Sales Aggregator.

#### 1) Postgres Connector

Java 17 application that registers a Debezium PostgreSQL CDC connector via Kafka Connect's REST API. It configures logical replication using the `pgoutput` plugin with a dedicated replication slot (`sales_slot`) and publication (`sales_publication`). It captures changes from four tables (sales, products, salesmen, stores) and publishes them to `electromart.public.*` topics. The `ExtractNewRecordState` transform unwraps the Debezium envelope into plain JSON.

#### 2) CSV Connector

Java 17 application using Kafka Clients 3.7.0 and MinIO Client 8.5.7. It runs an embedded HTTP server on port 8085 that receives webhook notifications from MinIO when new CSV files land in the `sales-csv` bucket. On each event, it downloads the file, parses each CSV row into a JSON record, publishes to the `raw_csv` topic, then moves the processed file to `sales-csv-processed`. A fallback polling mechanism (every 10 seconds) catches any missed webhook events.

#### 3) SOAP Connector

Java 17 application using Kafka Clients 3.7.0 and built-in Java XML parsers. It polls the SOAP endpoint every 5 seconds using cursor-based pagination (page size of 100). Each SOAP XML response is parsed, individual `<sale:record>` elements are mapped to JSON fields, and the records are published to the `raw_soap` topic.

### Processing Connectors

Beyond source connectors, the pipeline includes processing connectors that transform and enrich data already in Kafka rather than pulling from external systems.

#### 1) Postgres Enricher

Java 17 Kafka Streams application (v3.7.0) that enriches raw CDC events from PostgreSQL. It consumes the `electromart.public.sales` stream and joins it with three GlobalKTables (`electromart.public.products`, `electromart.public.salesmen`, `electromart.public.stores`) to denormalize each sale with full product, salesman, and store details. It also normalizes the schema — converting numeric `sale_id` to `"PG-{id}"` format, timestamps to ISO-8601, and removing internal DB columns (`created_at`, `updated_at`, foreign keys). The enriched records are published to `raw_postgres`.

#### 2) Sales Aggregator

Java 17 Kafka Streams application (v3.7.0) that acts as the pipeline's normalizer and gatekeeper. It consumes from three source-specific topics (`raw_csv`, `raw_soap`, `raw_postgres`), adds normalization metadata (`trace_id`, `source`, `source_version`, `ingested_at`) to CSV and SOAP records, validates each record against the canonical schema (required fields: `sale_id`, `source`, `sale_timestamp`, `total_amount`), and branches valid records to the `sales` topic while routing invalid ones to `sales-dlq` (dead letter queue). Valid records are then consumed by `sales-consumer`, which persists them in TimescaleDB.

### Kafka

Apache Kafka 3.7.1 running in KRaft mode (no ZooKeeper) as a single broker. It serves as the central event streaming backbone connecting all data sources, processing stages, and storage. Key application topics: `raw_csv`, `raw_soap`, `raw_postgres` (source-specific raw input), `sales` (normalized/validated output), `sales-dlq` (dead letter queue), and `electromart.public.*` (CDC topics from Debezium). Topics are auto-created by connectors with 1–3 partitions and replication factor 1.

#### Kafka Connector

Debezium Connect 2.5, deployed as the Kafka Connect runtime. It hosts the Debezium PostgreSQL connector plugin and manages connector lifecycle, offset tracking, and schema handling. Internal state is stored in three compacted topics: `_connect-configs`, `_connect-offsets`, and `_connect-status`. It exposes a REST API on port 8083 for connector registration and management.

#### Kafka UI

Kafka UI v0.7.2 (ProVectus Labs), a web-based interface on port 8888 for monitoring the Kafka cluster. It provides visibility into topics, messages, consumer groups, and Kafka Connect connectors. It connects to both the Kafka broker and Kafka Connect to give a unified operational view of the streaming infrastructure.

### Storage

#### TimescaleDB

TimescaleDB (latest, on PostgreSQL 15) running on port 5433 (credentials: sales/sales123, database: salesdb). It extends PostgreSQL with time-series optimizations — the `sales` table is a hypertable partitioned by `sale_timestamp`, enabling efficient time-range queries and automatic chunk management. Three continuous aggregates (`top_products`, `top_cities`, `top_salesmen`) pre-compute hourly revenue rollups and auto-refresh every 5 minutes. TimescaleDB was chosen over regular PostgreSQL for its automatic time partitioning, continuous aggregates, and built-in compression for historical data.

## Results

The project delivers two categories of results: business analytics (sales performance dashboards) and operational observability (pipeline health monitoring). All results are served through Grafana dashboards backed by TimescaleDB queries and Prometheus metrics.

### Prometheus

Prometheus v2.49.1 scrapes metrics every 15 seconds from three exporters: `kafka-exporter` (topic offsets, consumer lag, partition counts), `postgres-exporter` v0.15.0 (source database connections and query stats), and `timescaledb-exporter` v0.15.0 (warehouse table sizes and performance). Data is retained for 7 days and feeds into Grafana's pipeline observability dashboard for infrastructure-level monitoring.

### Grafana

Grafana 10.3.1 with two provisioned datasources (TimescaleDB as default, Prometheus for infra metrics) and two pre-configured dashboards. The **Sales Dashboard** shows top 10 salesmen, cities, and products by revenue plus revenue-by-source breakdowns using TimescaleDB continuous aggregates. The **Pipeline Observability Dashboard** uses Prometheus metrics to display Kafka topic ingestion rates, consumer lag, broker health, and database connection stats.
