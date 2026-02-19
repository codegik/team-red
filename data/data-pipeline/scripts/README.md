# Kappa Pipeline Scripts

This directory contains scripts for building, starting, testing, and managing the Kappa Architecture Data Pipeline.

## Quick Start

**Full automated test (build, start, test, cleanup):**
```bash
./scripts/full-test.sh
```

**Manual workflow:**
```bash
./scripts/build-all.sh          # Build all services
./scripts/start-pipeline.sh     # Start infrastructure and services
./scripts/run-tests.sh          # Run end-to-end tests
./scripts/cleanup.sh            # Stop services (preserves data)
./scripts/cleanup.sh --volumes  # Stop services and remove all data
```

## Available Scripts

### Build & Start

**`build-all.sh`**
- Builds all Maven modules
- Builds all Docker images
- Usage: `./scripts/build-all.sh`

**`start-pipeline.sh`**
- Starts all services with Docker Compose
- Waits for services to be ready
- Creates Kafka topics
- Restarts stream processors for stability
- Usage: `./scripts/start-pipeline.sh`

**`wait-for-services.sh`**
- Health checks for all infrastructure services
- Called automatically by `start-pipeline.sh`
- Usage: `./scripts/wait-for-services.sh`

**`create-topics.sh`**
- Creates required Kafka topics
- Called automatically by `start-pipeline.sh`
- Usage: `./scripts/create-topics.sh`

### Testing

**`run-tests.sh`**
- Runs comprehensive end-to-end tests
- Verifies all containers are running
- Checks Kafka topics exist
- Generates test data
- Verifies data flows through CDC → Kafka → Stream Processors → TimescaleDB
- Tests Query API endpoints
- Usage: `./scripts/run-tests.sh`

**`full-test.sh`**
- Complete automated test suite
- Builds everything from scratch
- Starts pipeline
- Runs tests
- Cleans up after completion
- Usage: `./scripts/full-test.sh [--no-cleanup]`
- Options:
  - `--no-cleanup`: Leave services running after tests

### Data Generation

**`generate-db-sales.sh <count>`**
- Generates test sales data in PostgreSQL
- Usage: `./scripts/generate-db-sales.sh 100`
- Example: Generate 100 sales records

**`generate-file-sales.sh <count>`**
- Generates test sales data as CSV/JSON files
- Usage: `./scripts/generate-file-sales.sh 50`
- Example: Generate 50 sales records in files

### Cleanup

**`stop-pipeline.sh`**
- Stops all services (preserves data)
- Usage: `./scripts/stop-pipeline.sh`

**`cleanup.sh`**
- Stops services with optional volume removal
- Usage: `./scripts/cleanup.sh [--volumes]`
- Options:
  - `-v, --volumes`: Remove all data volumes

## Test Workflow

### Quick Test (5 minutes)
```bash
./scripts/full-test.sh
```

### Manual Test with Inspection
```bash
./scripts/build-all.sh
./scripts/start-pipeline.sh
./scripts/generate-db-sales.sh 100
sleep 15

podman exec timescaledb psql -U analyticsuser -d analyticsdb \
  -c "SELECT city, total_sales, transaction_count FROM top_sales_by_city ORDER BY total_sales DESC LIMIT 5;"

curl "http://localhost:8090/api/v1/sales/by-city?size=5" | jq

./scripts/cleanup.sh --volumes
```

### Development Workflow
```bash
./scripts/start-pipeline.sh --no-cleanup

./scripts/run-tests.sh

./scripts/cleanup.sh
```

## Service Access Points

After starting the pipeline:

- **Query API**: http://localhost:8090
- **API Documentation**: http://localhost:8090/swagger-ui.html
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090

## Troubleshooting

**Stream processors not running:**
```bash
podman logs stream-processor-city
podman logs stream-processor-salesman
podman restart stream-processor-city stream-processor-salesman
```

**Check service status:**
```bash
podman ps
podman compose ps
```

**View logs:**
```bash
podman logs db-connector-service
podman logs stream-processor-city
podman logs query-api
```

**Reset everything:**
```bash
./scripts/cleanup.sh --volumes
./scripts/start-pipeline.sh
```
