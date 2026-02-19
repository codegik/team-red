# Kappa Pipeline Quick Start

## One-Command Test

Run the complete test suite (build, start, test, cleanup):

```bash
./scripts/full-test.sh
```

This will:
1. Build all services
2. Start the pipeline
3. Run end-to-end tests
4. Stop and clean up everything

## Manual Workflow

### Start the pipeline:
```bash
./scripts/start-pipeline.sh
```

### Run tests:
```bash
./scripts/run-tests.sh
```

### Stop the pipeline:
```bash
./scripts/cleanup.sh              # Preserves data
./scripts/cleanup.sh --volumes    # Removes all data
```

## What Gets Tested

The test suite verifies:

1. **Infrastructure**: All containers running (Kafka, PostgreSQL, TimescaleDB, etc.)
2. **Kafka Topics**: Required topics exist (sales.raw.db, sales.raw.file, sales.raw.soap)
3. **Data Ingestion**: CDC captures changes from PostgreSQL
4. **Stream Processing**: Data flows to TimescaleDB aggregation tables
5. **Query API**: REST endpoints return correct data
6. **Data Quality**: Top city and salesman rankings computed correctly

## Access Points

After starting:
- Query API: http://localhost:8090
- API Docs: http://localhost:8090/swagger-ui.html
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090

## Sample Queries

```bash
curl "http://localhost:8090/api/v1/sales/by-city?size=5" | jq
curl "http://localhost:8090/api/v1/sales/top-salesman?limit=5" | jq
```

## Generate More Data

```bash
./scripts/generate-db-sales.sh 1000
./scripts/generate-file-sales.sh 500
```

## Troubleshooting

View logs:
```bash
podman logs db-connector-service
podman logs stream-processor-city
podman logs stream-processor-salesman
```

Check status:
```bash
podman ps
```

Restart stream processors:
```bash
podman restart stream-processor-city stream-processor-salesman
```

## Architecture

```
PostgreSQL (source) → Debezium CDC → Kafka → Stream Processors → TimescaleDB → REST API
     ↓                                 ↓
File System                      Data Lineage
     ↓                                 ↓
SOAP Service                    Observability (Prometheus/Grafana)
```

## Requirements

- Java 25
- Maven
- Podman or Docker
- 8GB+ RAM recommended

## More Information

See [scripts/README.md](scripts/README.md) for detailed script documentation.
