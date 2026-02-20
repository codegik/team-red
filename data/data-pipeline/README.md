# Kappa Architecture Data Pipeline

## Challenge 

- 23.MAR.2026 - Data Kata
Must Create a Modern Data Pipeline with:
	1. Ingestion for 3 different data sources (Relational DB, File system and Traditional WS-*)
	2. Modern Processing with Spark, Flink or Kafka Streams
	3. Data Lineage
	4. Observability
	5. Pipelines must have at least 2 pipelines:
		a. Top Sales per City 
		b. Top Salesman in the whole country
	6. The final Aggregated results mut be in a dedicated DB and API
	7. Restrictions:
		a. Python
		b. Red-Shift
		c. Hadoop

A real-time data processing pipeline implementing the Kappa Architecture pattern with Apache Kafka, Java 25, Spring Boot 4.0, and TimescaleDB.

## Table of Contents
- [Architecture Overview](#architecture-overview)
- [Data Flow](#data-flow)
- [Components](#components)
- [Technologies](#technologies)
- [Getting Started](#getting-started)

---

## Architecture Overview

The Kappa Architecture is a streamlined approach to system design focused on **real-time data processing only**. Unlike Lambda Architecture, Kappa eliminates the batch processing layer, treating all data as streams.

### Key Principles
- **Stream-First**: All data is processed as continuous streams
- **Replayability**: Historical data can be reprocessed by replaying event streams
- **Simplicity**: Single processing path reduces complexity and maintenance
- **Real-Time**: Low-latency processing for immediate insights

---

## Data Flow

```
┌─────────────────┐
│  Data Sources   │
│                 │
│ • PostgreSQL    │───┐
│ • CSV Files     │───┼─────► Change Data Capture (CDC)
│ • SOAP Service  │───┘       │
└─────────────────┘            │
                               ▼
                    ┌──────────────────────┐
                    │   Apache Kafka       │
                    │   (KRaft Mode)       │
                    │                      │
                    │  Topics:             │
                    │  • sales.raw.db      │
                    │  • sales.raw.file    │
                    │  • sales.raw.soap    │
                    └──────────┬───────────┘
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
    ┌───────────────────────┐   ┌───────────────────────┐
    │  Stream Processor     │   │  Stream Processor     │
    │  (City Aggregations)  │   │  (Salesman Stats)     │
    │                       │   │                       │
    │  • 5-min windows      │   │  • 5-min windows      │
    │  • Top products       │   │  • City coverage      │
    │  • Sales totals       │   │  • Sales totals       │
    └───────────┬───────────┘   └───────────┬───────────┘
                │                           │
                └────────────┬──────────────┘
                             ▼
                  ┌──────────────────────┐
                  │   TimescaleDB        │
                  │   (Time-Series DB)   │
                  │                      │
                  │  Tables:             │
                  │  • top_sales_by_city │
                  │  • top_salesman      │
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │    Query API         │
                  │  (Spring Boot REST)  │
                  │                      │
                  │  Endpoints:          │
                  │  • GET /by-city      │
                  │  • GET /top-salesman │
                  └──────────────────────┘
```

---

## Detailed Data Flow

### 1. Data Ingestion Layer

**Multiple data sources feed into the pipeline:**

```
PostgreSQL Source DB
    └─► Debezium CDC (db-connector service)
        └─► Captures INSERT/UPDATE/DELETE events
            └─► Publishes to Kafka topic: sales.raw.db

CSV/JSON Files
    └─► File Ingestion Service
        └─► Watches /data/input directory
            └─► Publishes to Kafka topic: sales.raw.file

SOAP Web Service
    └─► SOAP Connector Service
        └─► Polls SOAP endpoint periodically
            └─► Publishes to Kafka topic: sales.raw.soap
```

### 2. Stream Processing Layer

**Real-time aggregation using Kafka Streams:**

```
Kafka Topics (sales.raw.*)
    │
    ├─► Stream Processor: City Sales Aggregator
    │   │
    │   ├─► Windowing: 5-minute tumbling windows
    │   ├─► Grouping: By city
    │   ├─► Aggregation:
    │   │   • Total sales amount
    │   │   • Transaction count
    │   │   • Top product by sales
    │   │   • Top product sales amount
    │   │
    │   └─► Writes to: TimescaleDB.top_sales_by_city
    │
    └─► Stream Processor: Salesman Performance Aggregator
        │
        ├─► Windowing: 5-minute tumbling windows
        ├─► Grouping: By salesman_id
        ├─► Aggregation:
        │   • Total sales amount
        │   • Transaction count
        │   • Cities covered (distinct count)
        │   • Salesman name
        │
        └─► Writes to: TimescaleDB.top_salesman_country
```

### 3. Storage Layer

**TimescaleDB (PostgreSQL with time-series extensions):**

```sql
-- City Sales Table
top_sales_by_city (
    id SERIAL PRIMARY KEY,
    city VARCHAR(255),
    window_start TIMESTAMPTZ,
    window_end TIMESTAMPTZ,
    total_sales DOUBLE PRECISION,
    transaction_count INTEGER,
    top_product VARCHAR(255),
    top_product_sales DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW()
)

-- Salesman Performance Table
top_salesman_country (
    id SERIAL PRIMARY KEY,
    salesman_id VARCHAR(50),
    salesman_name VARCHAR(255),
    window_start TIMESTAMPTZ,
    window_end TIMESTAMPTZ,
    total_sales DOUBLE PRECISION,
    transaction_count INTEGER,
    cities_covered INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW()
)
```

### 4. Serving Layer

**REST API for querying aggregated data:**

```
GET /api/v1/sales/by-city?city={city}&size={size}
    └─► Returns paginated city sales aggregations
    └─► Sorted by total_sales DESC

GET /api/v1/sales/top-salesman?limit={limit}
    └─► Returns top performing salespeople
    └─► Sorted by total_sales DESC
```

---

## Components

### Infrastructure Services

| Service | Technology | Port | Purpose |
|---------|-----------|------|---------|
| **Kafka** | Confluent Platform 7.7.0 (KRaft) | 9092 | Message broker for event streaming |
| **Schema Registry** | Confluent 7.7.0 | 8081 | Avro schema management |
| **PostgreSQL** | PostgreSQL 17 | 5432 | Source database for CDC |
| **TimescaleDB** | TimescaleDB (PostgreSQL 17) | 5433 | Time-series analytics storage |
| **Prometheus** | Prometheus Latest | 9090 | Metrics collection |
| **Grafana** | Grafana Latest | 3000 | Metrics visualization |
| **Loki** | Grafana Loki Latest | 3100 | Log aggregation |

### Application Services

| Service | Technology | Port | Purpose |
|---------|-----------|------|---------|
| **db-connector** | Java 25 + Debezium 2.7.3 | - | CDC from PostgreSQL to Kafka |
| **file-ingestion** | Java 25 | - | File monitoring and ingestion |
| **soap-connector** | Java 25 | - | SOAP service polling |
| **stream-processor-city** | Java 25 + Kafka Streams 3.8.1 | - | City sales aggregation |
| **stream-processor-salesman** | Java 25 + Kafka Streams 3.8.1 | - | Salesman performance aggregation |
| **query-api** | Spring Boot 4.0 | 8090 | REST API for queries |
| **lineage-tracker** | Java 25 | - | Data lineage tracking |
| **soap-mock-service** | Spring Boot 4.0 | 8080 | Mock SOAP service for testing |

---

## Technologies

### Core Stack
- **Java 25** - Latest LTS with modern language features
- **Spring Boot 4.0.0** - Application framework
- **Apache Kafka 3.8.1** - Event streaming platform (KRaft mode)
- **Kafka Streams 3.8.1** - Stream processing library
- **Debezium 2.7.3** - Change Data Capture
- **TimescaleDB** - Time-series database
- **PostgreSQL 17** - Relational database

### Build & Deployment
- **Maven 3.9+** - Build tool
- **Podman/Docker** - Containerization
- **Docker Compose** - Orchestration

### Observability
- **Prometheus** - Metrics collection
- **Grafana** - Dashboards and visualization
- **Loki** - Log aggregation
- **Micrometer** - Application metrics

---

## Key Features

### Change Data Capture (CDC)
- **Real-time** event capture from PostgreSQL
- **Exactly-once** semantics with Debezium
- **Schema evolution** support via Avro

### Stream Processing
- **Stateful processing** with Kafka Streams
- **Time windowing** (5-minute tumbling windows)
- **Exactly-once** processing guarantees
- **RocksDB** state stores for aggregations

### Data Lineage
- **Event tracking** via Kafka headers
- **Source identification** (db, file, soap)
- **Timestamp tracking** for audit trails

### Scalability
- **Horizontal scaling** of stream processors
- **Kafka partitioning** for parallel processing
- **TimescaleDB** for efficient time-series queries

---

## Getting Started

### Prerequisites
- Java 25
- Maven 3.9+
- Podman or Docker
- 8GB+ RAM recommended

### Quick Start

```bash
./scripts/full-test.sh
```

This single command will:
1. Build all services
2. Start the pipeline
3. Run 18 automated tests
4. Clean up everything

For detailed instructions, see [QUICKSTART.md](QUICKSTART.md).

---

## Project Structure

```
.
├── common/                    # Shared models, utilities, and Avro schemas
├── services/
│   ├── db-connector/         # Debezium CDC service
│   ├── file-ingestion/       # File monitoring service
│   ├── soap-connector/       # SOAP polling service
│   ├── stream-processor/     # Kafka Streams aggregators
│   ├── query-api/            # REST API service
│   └── lineage-tracker/      # Data lineage tracking
├── mock-services/
│   └── soap-service/         # Mock SOAP service for testing
├── scripts/                   # Build, start, test, and cleanup scripts
├── init-scripts/             # Database initialization SQL
├── observability/            # Prometheus and Grafana configs
├── docker-compose.yml        # Infrastructure definition
└── pom.xml                   # Maven parent POM
```

---

## Architecture Advantages

### Why Kappa Over Lambda?

| Aspect | Kappa | Lambda |
|--------|-------|--------|
| **Complexity** | Single processing path | Batch + streaming layers |
| **Code** | One codebase | Duplicate logic in batch/stream |
| **Latency** | Real-time only | Batch lag + real-time |
| **Reprocessing** | Replay streams | Re-run batch jobs |
| **Maintenance** | Simpler | Higher complexity |

### When to Use Kappa

✅ **Good Fit:**
- Real-time analytics requirements
- Event-driven architectures
- Systems where all data can be treated as streams
- Need for data reprocessing via replay

❌ **Not Ideal:**
- Complex batch ETL requirements
- Historical data cannot be streamed
- Batch-only processing is sufficient

---

## Monitoring & Observability

### Metrics (Prometheus)
- Stream processor lag
- Kafka consumer offsets
- TimescaleDB write throughput
- API request rates and latency

### Logs (Loki)
- Application logs from all services
- Kafka broker logs
- Database logs

### Dashboards (Grafana)
- Real-time pipeline health
- Data flow visualization
- Performance metrics

Access at: http://localhost:3000 (admin/admin)

---

## Testing

The pipeline includes comprehensive automated testing:

```bash
./scripts/run-tests.sh
```

**18 automated tests verify:**
- ✓ All containers running
- ✓ Kafka topics exist
- ✓ Data ingestion (50 test records)
- ✓ CDC capturing events
- ✓ Stream processing (city & salesman aggregations)
- ✓ TimescaleDB data persistence
- ✓ Query API endpoints
- ✓ Data quality and correctness

---

## License

Internal project - Team Red Data Engineering

---

## Documentation

- [Quick Start Guide](QUICKSTART.md) - Get up and running in 5 minutes
- [API Documentation](http://localhost:8090/swagger-ui.html) - Interactive API docs (when running)

---

## Support

For issues or questions, contact the Team Red Data Engineering team.
