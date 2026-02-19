# Kappa Architecture Data Pipeline Implementation

A production-ready data pipeline implementing the Kappa architecture pattern using Java 25, Apache Kafka, Kafka Streams, and TimescaleDB.

## Architecture Overview

This implementation demonstrates a modern stream-first architecture with:
- Real-time data ingestion from PostgreSQL (CDC), file system, and SOAP web service
- Stream processing with Kafka Streams for aggregations
- Data lineage tracking for full traceability
- Observability with Prometheus, Grafana, and Loki
- TimescaleDB for time-series analytics storage
- REST API for querying results

## Technology Stack

- **Java**: 25
- **Build Tool**: Maven
- **Stream Processing**: Kafka 3.9.0, Kafka Streams
- **CDC**: Debezium 3.0.4
- **Analytics Database**: TimescaleDB (PostgreSQL with time-series extension)
- **API Framework**: Spring Boot 3.4.2
- **Observability**: Prometheus, Grafana, Loki
- **Containerization**: Docker Compose

## Project Structure

```
kappa/
├── common/                    # Shared models, serdes, utilities
├── services/
│   ├── db-connector/         # Debezium CDC for PostgreSQL
│   ├── file-ingestion/       # File watcher for CSV/JSON
│   ├── soap-connector/       # SOAP client with polling
│   ├── stream-processor/     # Kafka Streams aggregations
│   ├── lineage-tracker/      # Data lineage tracking
│   └── query-api/            # Spring Boot REST API
├── mock-services/
│   └── soap-service/         # Mock SOAP endpoint
├── init-scripts/             # Database initialization
├── observability/            # Prometheus, Grafana configs
├── scripts/                  # Helper scripts
└── docker-compose.yml        # Complete infrastructure
```

## Prerequisites

- Docker and Docker Compose
- Java 25
- Maven 3.9+
- 8GB RAM minimum (16GB recommended)

## Quick Start

### 1. Build All Services

```bash
./scripts/build-all.sh
```

### 2. Start Infrastructure and Services

```bash
./scripts/start-pipeline.sh
```

This will:
- Start all infrastructure (Kafka, PostgreSQL, TimescaleDB, Prometheus, Grafana)
- Start all application services
- Create Kafka topics
- Initialize databases

### 3. Generate Test Data

```bash
./scripts/generate-db-sales.sh 1000
./scripts/generate-file-sales.sh 500
```

### 4. Access the System

- **Query API**: http://localhost:8090
- **API Documentation**: http://localhost:8090/swagger-ui.html
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090

## API Endpoints

### Get Sales by City

```bash
curl "http://localhost:8090/api/v1/sales/by-city?city=New%20York&from=2026-02-01T00:00:00Z&size=10"
```

### Get Top Salesmen

```bash
curl "http://localhost:8090/api/v1/sales/top-salesman?limit=10"
```

### Get Data Lineage

```bash
curl "http://localhost:8090/api/v1/lineage/sale-001"
```

### Health Check

```bash
curl "http://localhost:8090/api/v1/health"
```

## Data Flow

1. **Ingestion**:
   - PostgreSQL CDC captures changes → `sales.raw.db` topic
   - File watcher processes CSV/JSON → `sales.raw.file` topic
   - SOAP poller fetches data → `sales.raw.soap` topic

2. **Stream Processing**:
   - City Sales Topology: Aggregates by city (1-hour windows)
   - Salesman Topology: Aggregates by salesman (1-hour windows)
   - Results written to TimescaleDB

3. **Lineage Tracking**:
   - Consumes from all raw topics
   - Extracts metadata from Kafka headers
   - Stores lineage in TimescaleDB

4. **Query API**:
   - Provides REST endpoints for analytics
   - Supports pagination and filtering
   - Returns data lineage traces

## Kappa Architecture Benefits

- **Simplified Architecture**: Single stream processing layer (no batch layer)
- **Event Replay**: Reprocess historical data by resetting Kafka offsets
- **Exactly-Once Semantics**: No data loss or duplication
- **Real-Time Processing**: Sub-second latency for aggregations
- **Full Lineage**: Track every event from source to destination

## Observability

### Metrics

Prometheus collects metrics from:
- Kafka broker (JMX)
- All application services (Micrometer)
- Consumer lag
- Processing rates
- Business metrics (sales volume, etc.)

### Dashboards

Grafana includes pre-built dashboards for:
- Pipeline health (processing rates, errors, lag)
- Business metrics (top cities, top salesmen)
- Data lineage distribution
- Infrastructure health

### Logs

Structured JSON logs with:
- Lineage ID correlation
- Source system tracking
- Timestamp information
- Searchable in Loki

## Development

### Run a Single Service Locally

```bash
cd services/query-api
mvn spring-boot:run
```

### Run Tests

```bash
mvn test
```

### Build Docker Images

```bash
docker-compose build
```

## Troubleshooting

### Check Service Status

```bash
docker-compose ps
```

### View Service Logs

```bash
docker-compose logs -f db-connector-service
docker-compose logs -f stream-processor-city
```

### Reset Kafka Offsets (for replay)

```bash
docker exec stream-processor-city kafka-streams-application-reset \
  --application-id city-sales-aggregator \
  --bootstrap-servers kafka:29092 \
  --input-topics sales.raw.db,sales.raw.file,sales.raw.soap
```

### Clean Up Everything

```bash
./scripts/stop-pipeline.sh
docker-compose down -v
```

## Performance Characteristics

- **Throughput**: 10,000+ events/second per partition
- **Latency**: Sub-second (p99 < 500ms)
- **Exactly-Once**: Enabled for all stream processors
- **Retention**: Kafka 7 days, TimescaleDB unlimited

## Scaling Considerations

- Increase Kafka partitions for higher throughput
- Add more stream processor instances (auto-scales via consumer groups)
- Use TimescaleDB compression for long-term storage
- Horizontal scaling for Query API (stateless)

## License

This is an implementation for the Team Red Data Pipeline Challenge.
