# Testing and Verification Guide

This guide provides comprehensive testing procedures for the Kappa Architecture Data Pipeline.

## Prerequisites

Ensure the pipeline is built and running:

```bash
./scripts/build-all.sh
./scripts/start-pipeline.sh
```

## 1. Infrastructure Health Check

### Verify All Services are Running

```bash
docker-compose ps
```

Expected: All services should show "Up" status.

### Check Kafka Topics

```bash
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

Expected topics:
- sales.raw.db
- sales.raw.file
- sales.raw.soap

### Check Database Connectivity

```bash
docker exec postgres-source psql -U sourceuser -d sourcedb -c "SELECT COUNT(*) FROM sales;"
docker exec timescaledb psql -U analyticsuser -d analyticsdb -c "SELECT COUNT(*) FROM top_sales_by_city;"
```

## 2. Data Ingestion Testing

### Test PostgreSQL CDC

Insert a test record:

```bash
docker exec postgres-source psql -U sourceuser -d sourcedb -c \
  "INSERT INTO sales VALUES ('test-db-001', $(date +%s)000, 'sm-test', 'Test Salesman', 'cust-999', 'prod-test', 'Test Product', 1, 100.0, 100.0, 'Test City', 'USA');"
```

Verify in Kafka:

```bash
docker exec kafka kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic sales.raw.db --from-beginning --max-messages 1 --timeout-ms 5000
```

### Test File Ingestion

Generate CSV file:

```bash
./scripts/generate-file-sales.sh 10
```

Check file was processed:

```bash
ls -l data/archive/
docker logs file-ingestion-service | grep "Sent sale event"
```

### Test SOAP Connector

Check SOAP service logs:

```bash
docker logs soap-connector-service | grep "Received"
```

Verify SOAP mock service is generating data:

```bash
curl http://localhost:8080/ws/sales.wsdl
```

## 3. Stream Processing Verification

### Check Stream Processor Logs

City aggregations:

```bash
docker logs stream-processor-city | grep "City sales aggregated"
```

Salesman aggregations:

```bash
docker logs stream-processor-salesman | grep "Salesman sales aggregated"
```

### Verify Data in TimescaleDB

```bash
docker exec timescaledb psql -U analyticsuser -d analyticsdb -c \
  "SELECT city, SUM(total_sales), COUNT(*) FROM top_sales_by_city GROUP BY city ORDER BY SUM(total_sales) DESC LIMIT 10;"
```

```bash
docker exec timescaledb psql -U analyticsuser -d analyticsdb -c \
  "SELECT salesman_name, SUM(total_sales), COUNT(*) FROM top_salesman_country GROUP BY salesman_name ORDER BY SUM(total_sales) DESC LIMIT 10;"
```

## 4. Data Lineage Testing

### Check Lineage Tracking

```bash
docker logs lineage-tracker | grep "Tracked lineage"
```

### Verify Lineage in Database

```bash
docker exec timescaledb psql -U analyticsuser -d analyticsdb -c \
  "SELECT lineage_id, sale_id, source_system, kafka_topic FROM data_lineage LIMIT 5;"
```

## 5. REST API Testing

### Health Check

```bash
curl http://localhost:8090/api/v1/health
```

Expected: `{"status":"UP","service":"query-api","timestamp":"..."}`

### Query Sales by City

```bash
curl "http://localhost:8090/api/v1/sales/by-city?city=New%20York&size=5" | jq
```

### Query Top Salesmen

```bash
curl "http://localhost:8090/api/v1/sales/top-salesman?limit=10" | jq
```

### Query Data Lineage

First, get a sale ID:

```bash
SALE_ID=$(docker exec timescaledb psql -U analyticsuser -d analyticsdb -t -c \
  "SELECT sale_id FROM data_lineage LIMIT 1;" | tr -d ' ')
```

Then query lineage:

```bash
curl "http://localhost:8090/api/v1/lineage/$SALE_ID" | jq
```

## 6. Performance Testing

### Load Test - Database Ingestion

```bash
./scripts/generate-db-sales.sh 10000
```

Monitor consumer lag:

```bash
docker exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group city-sales-aggregator
```

### Load Test - File Ingestion

Generate multiple files:

```bash
for i in {1..10}; do
  ./scripts/generate-file-sales.sh 100 &
done
wait
```

### Check Processing Rate

```bash
docker logs stream-processor-city | grep "aggregated" | tail -20
```

## 7. End-to-End Test

### Complete Data Flow Test

1. Insert a uniquely identifiable record:

```bash
TEST_SALE_ID="e2e-test-$(date +%s)"
docker exec postgres-source psql -U sourceuser -d sourcedb -c \
  "INSERT INTO sales VALUES ('$TEST_SALE_ID', $(date +%s)000, 'sm-e2e', 'E2E Test', 'cust-e2e', 'prod-e2e', 'E2E Product', 5, 200.0, 1000.0, 'E2E City', 'USA');"
```

2. Wait for processing (5 seconds):

```bash
sleep 5
```

3. Query the result in TimescaleDB:

```bash
docker exec timescaledb psql -U analyticsuser -d analyticsdb -c \
  "SELECT * FROM top_sales_by_city WHERE city = 'E2E City' ORDER BY window_start DESC LIMIT 1;"
```

4. Check lineage:

```bash
curl "http://localhost:8090/api/v1/lineage/$TEST_SALE_ID" | jq
```

Expected: Full lineage trace with source system, timestamps, and Kafka metadata.

## 8. Observability Testing

### Prometheus Metrics

```bash
curl http://localhost:9090/api/v1/targets | jq
```

Expected: All targets should be "UP".

Query metrics:

```bash
curl -G http://localhost:9090/api/v1/query \
  --data-urlencode 'query=kafka_server_broker_topic_metrics_messages_in_total' | jq
```

### Grafana Dashboard

Open http://localhost:3000 (admin/admin)

Verify:
1. Pipeline Overview dashboard shows data
2. Metrics are being collected
3. No error spikes

### Loki Logs

Query logs via Grafana Explore or API:

```bash
curl -G http://localhost:3100/loki/api/v1/query \
  --data-urlencode 'query={application="kappa-pipeline"}' | jq
```

## 9. Failure Recovery Testing

### Test Stream Processor Restart

Stop processor:

```bash
docker stop stream-processor-city
```

Insert data:

```bash
./scripts/generate-db-sales.sh 100
```

Restart processor:

```bash
docker start stream-processor-city
```

Verify: Processor catches up and processes all missed events.

### Test Kafka Offset Reset (Reprocessing)

Reset to replay last hour:

```bash
docker exec stream-processor-city kafka-streams-application-reset \
  --application-id city-sales-aggregator \
  --bootstrap-servers kafka:29092 \
  --to-datetime $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S.000)
```

## 10. Data Quality Checks

### Check for Duplicates

```bash
docker exec timescaledb psql -U analyticsuser -d analyticsdb -c \
  "SELECT city, window_start, COUNT(*) FROM top_sales_by_city GROUP BY city, window_start HAVING COUNT(*) > 1;"
```

Expected: No results (exactly-once semantics).

### Verify Lineage Completeness

```bash
docker exec timescaledb psql -U analyticsuser -d analyticsdb -c \
  "SELECT COUNT(*) FROM data_lineage WHERE lineage_id IS NULL OR sale_id IS NULL;"
```

Expected: 0 rows with NULL values.

## Test Results Summary

Create a test report with:

- Infrastructure status: PASS/FAIL
- Data ingestion: PASS/FAIL
- Stream processing: PASS/FAIL
- Data lineage: PASS/FAIL
- API endpoints: PASS/FAIL
- Observability: PASS/FAIL
- Performance: Throughput metrics
- Failure recovery: PASS/FAIL

## Cleanup After Testing

```bash
./scripts/stop-pipeline.sh
docker-compose down -v
```

This removes all containers, volumes, and test data.
