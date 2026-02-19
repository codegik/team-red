# Quick Start Guide

Get the Kappa Architecture Data Pipeline running in 5 minutes.

## Prerequisites

- **Java 25** - [Install via SDKMAN](https://sdkman.io/)
- **Maven 3.9+** - Build tool
- **Podman or Docker** - Container runtime
- **8GB+ RAM** - Recommended for all services

Verify installation:
```bash
java -version   # Should show Java 25
mvn -version    # Should show Maven 3.9+
podman --version # or docker --version
```

---

## One-Command Test

Run the complete test suite from scratch:

```bash
./scripts/full-test.sh
```

This will:
1. ✅ Build all services (Maven compile)
2. ✅ Start infrastructure (Kafka, PostgreSQL, TimescaleDB)
3. ✅ Deploy application services
4. ✅ Run 18 automated tests
5. ✅ Clean up everything

**Time:** ~5-7 minutes

---

## Manual Workflow

### 1. Start the Pipeline

```bash
./scripts/start-pipeline.sh
```

**What it does:**
- Starts 13 containers (Kafka, databases, services)
- Waits for all services to be healthy
- Creates Kafka topics
- Restarts stream processors for stability

**Output:**
```
Pipeline is ready!

Access points:
  - Query API: http://localhost:8090
  - API Docs: http://localhost:8090/swagger-ui.html
  - Grafana: http://localhost:3000 (admin/admin)
  - Prometheus: http://localhost:9090
```

**Time:** ~2-3 minutes

---

### 2. Generate Test Data

**Option A: Database (PostgreSQL)**
```bash
./scripts/generate-db-sales.sh 100
```
Inserts 100 sales records into PostgreSQL. CDC will capture and stream them to Kafka.

**Option B: Files (CSV/JSON)**
```bash
./scripts/generate-file-sales.sh 50
```
Creates 50 CSV/JSON files in `/data/input`. File ingestion service will process them.

**Time:** ~5 seconds

---

### 3. Verify Data Flow

**Wait for processing:**
```bash
sleep 15  # Give stream processors time to aggregate
```

**Check TimescaleDB:**
```bash
podman exec timescaledb psql -U analyticsuser -d analyticsdb \
  -c "SELECT city, total_sales, transaction_count, top_product
      FROM top_sales_by_city
      ORDER BY total_sales DESC
      LIMIT 5;"
```

**Expected output:**
```
     city      | total_sales | transaction_count | top_product
---------------+-------------+-------------------+-------------
 San Francisco |    89718.58 |                20 | USB Cable
 Seattle       |    83043.38 |                15 | Desk
 Austin        |    56381.29 |                 9 | Monitor
```

---

### 4. Query the API

**Top cities by sales:**
```bash
curl "http://localhost:8090/api/v1/sales/by-city?size=5" | jq
```

**Top salespeople:**
```bash
curl "http://localhost:8090/api/v1/sales/top-salesman?limit=5" | jq
```

**Interactive API docs:**
Open http://localhost:8090/swagger-ui.html in your browser

---

### 5. Run Tests

```bash
./scripts/run-tests.sh
```

**18 automated tests verify:**
- ✓ All containers running
- ✓ Kafka topics exist
- ✓ Test data generation
- ✓ CDC capturing events
- ✓ Stream processing working
- ✓ Data in TimescaleDB
- ✓ API endpoints responding
- ✓ Data quality checks

**Output:**
```
========================================
Test Results
========================================
Passed: 18
Failed: 0

All tests passed successfully!
```

**Time:** ~30 seconds

---

### 6. Stop the Pipeline

**Preserve data:**
```bash
./scripts/cleanup.sh
```
Stops containers but keeps data volumes.

**Remove everything:**
```bash
./scripts/cleanup.sh --volumes
```
Stops containers and removes all data.

---

## Available Scripts

| Script | Purpose | Time |
|--------|---------|------|
| `./scripts/full-test.sh` | Complete test from scratch | ~5-7 min |
| `./scripts/start-pipeline.sh` | Start all services | ~2-3 min |
| `./scripts/run-tests.sh` | Run tests (requires services running) | ~30 sec |
| `./scripts/generate-db-sales.sh <count>` | Generate test data in DB | ~5 sec |
| `./scripts/generate-file-sales.sh <count>` | Generate test files | ~5 sec |
| `./scripts/cleanup.sh` | Stop services (keep data) | ~10 sec |
| `./scripts/cleanup.sh --volumes` | Stop services (delete data) | ~10 sec |
| `./scripts/build-all.sh` | Build all Maven modules | ~30 sec |

---

## Development Workflow

**Iterative development cycle:**

```bash
# 1. Start once
./scripts/start-pipeline.sh

# 2. Make code changes...
# Edit files in services/

# 3. Rebuild specific service
mvn clean package -pl services/stream-processor -am -DskipTests
podman compose build stream-processor-city stream-processor-salesman
podman compose up -d stream-processor-city stream-processor-salesman

# 4. Test changes
./scripts/generate-db-sales.sh 50
sleep 15
./scripts/run-tests.sh

# 5. Repeat steps 2-4 as needed

# 6. Stop when done
./scripts/cleanup.sh
```

---

## Accessing Services

### Query API
- **Base URL:** http://localhost:8090
- **Swagger UI:** http://localhost:8090/swagger-ui.html
- **Health:** http://localhost:8090/actuator/health

### Grafana
- **URL:** http://localhost:3000
- **Credentials:** admin / admin
- **Dashboards:** Pre-configured with pipeline metrics

### Prometheus
- **URL:** http://localhost:9090
- **Metrics:** All application and infrastructure metrics

### Databases

**PostgreSQL (Source):**
```bash
podman exec -it postgres-source psql -U sourceuser -d sourcedb
```

**TimescaleDB (Analytics):**
```bash
podman exec -it timescaledb psql -U analyticsuser -d analyticsdb
```

---

## Viewing Logs

**All services:**
```bash
podman compose logs -f
```

**Specific service:**
```bash
podman logs -f db-connector-service
podman logs -f stream-processor-city
podman logs -f query-api
```

**Tail last 50 lines:**
```bash
podman logs --tail 50 stream-processor-city
```

---

## Troubleshooting

### Services not starting?

**Check container status:**
```bash
podman ps -a
```

**Restart specific service:**
```bash
podman restart stream-processor-city
```

### Stream processors crashing?

**Check logs for errors:**
```bash
podman logs stream-processor-city
podman logs stream-processor-salesman
```

**Common issues:**
- Topics not created → Run `./scripts/start-pipeline.sh` again
- TimescaleDB not ready → Wait 30 seconds, restart processors

### No data in TimescaleDB?

**Verify CDC is working:**
```bash
podman logs db-connector-service | grep "Sent sale event"
```

**Check Kafka topics have data:**
```bash
podman exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic sales.raw.db \
  --from-beginning \
  --max-messages 5
```

**Verify stream processors are running:**
```bash
podman ps --filter "name=stream-processor"
```

### Reset everything:

```bash
./scripts/cleanup.sh --volumes
./scripts/start-pipeline.sh
```

---

## What's Next?

- **Learn the architecture:** See [README.md](README.md) for detailed architecture diagrams
- **Explore the code:** Check `services/` directory for implementation details
- **Monitor the pipeline:** Open Grafana at http://localhost:3000
- **Query the data:** Use the API at http://localhost:8090/swagger-ui.html

---

## Common Use Cases

### Continuous Testing

```bash
# Leave services running, test repeatedly
./scripts/start-pipeline.sh
while true; do
  ./scripts/generate-db-sales.sh 50
  sleep 20
  ./scripts/run-tests.sh
  sleep 60
done
```

### Performance Testing

```bash
# Generate large dataset
./scripts/generate-db-sales.sh 1000
sleep 30
# Check TimescaleDB performance
podman exec timescaledb psql -U analyticsuser -d analyticsdb \
  -c "SELECT COUNT(*) FROM top_sales_by_city;"
```

### CI/CD Integration

```bash
# Single command for CI pipeline
./scripts/full-test.sh

# Exit code 0 = success, non-zero = failure
echo $?
```

---

## Getting Help

For issues or questions:
1. Check [README.md](README.md) for architecture details
2. Review logs: `podman compose logs`
3. Run tests: `./scripts/run-tests.sh`
4. Contact Team Red Data Engineering

---

## Summary

**Fastest path to success:**

```bash
# Everything in one command
./scripts/full-test.sh

# Or step by step
./scripts/start-pipeline.sh
./scripts/generate-db-sales.sh 100
sleep 15
curl "http://localhost:8090/api/v1/sales/by-city?size=5" | jq
./scripts/cleanup.sh
```

That's it! Your Kappa Architecture Data Pipeline is ready to use. 🚀
