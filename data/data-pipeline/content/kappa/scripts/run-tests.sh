#!/bin/bash

set -e

cd "$(dirname "$0")/.."

PASSED=0
FAILED=0

pass() {
    echo "✓ $1"
    PASSED=$((PASSED + 1))
}

fail() {
    echo "✗ $1"
    FAILED=$((FAILED + 1))
}

echo "Running Kappa Pipeline End-to-End Tests"
echo "========================================"
echo ""

echo "Test 1: Verify all containers are running"
echo "-------------------------------------------"

check_container() {
    local container=$1
    if podman ps --filter "name=$container" --format "{{.Names}}" | grep -q "$container"; then
        pass "$container is running"
        return 0
    else
        fail "$container is NOT running"
        return 1
    fi
}

check_container "kafka"
check_container "postgres-source"
check_container "timescaledb"
check_container "db-connector-service"
check_container "stream-processor-city"
check_container "stream-processor-salesman"
check_container "query-api"

echo ""
echo "Test 2: Verify Kafka topics exist"
echo "-------------------------------------------"

check_topic() {
    local topic=$1
    if podman exec kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep -q "^${topic}$"; then
        pass "Topic $topic exists"
        return 0
    else
        fail "Topic $topic does NOT exist"
        return 1
    fi
}

check_topic "sales.raw.db"
check_topic "sales.raw.file"
check_topic "sales.raw.soap"

echo ""
echo "Test 3: Generate test data"
echo "-------------------------------------------"

echo "Inserting 50 sales records into PostgreSQL..."
./scripts/generate-db-sales.sh 50 > /dev/null 2>&1
if [ $? -eq 0 ]; then
    pass "Generated 50 sales records in PostgreSQL"
else
    fail "Failed to generate sales records"
fi

echo ""
echo "Test 4: Wait for CDC to capture events"
echo "-------------------------------------------"
echo "Waiting 15 seconds for data to flow through pipeline..."
sleep 15

CDC_LOGS=$(podman logs --tail 100 db-connector-service 2>&1 | grep -c "Sent sale event" || echo "0")
if [ "$CDC_LOGS" -gt 0 ]; then
    pass "CDC captured events ($CDC_LOGS events logged)"
else
    fail "CDC did NOT capture any events"
fi

echo ""
echo "Test 5: Verify data in TimescaleDB"
echo "-------------------------------------------"

CITY_COUNT=$(podman exec timescaledb psql -U analyticsuser -d analyticsdb -t -c "SELECT COUNT(*) FROM top_sales_by_city;" 2>/dev/null | tr -d ' ')
if [ "$CITY_COUNT" -gt 0 ]; then
    pass "City aggregations exist in TimescaleDB (count: $CITY_COUNT)"
else
    fail "No city aggregations found in TimescaleDB"
fi

SALESMAN_COUNT=$(podman exec timescaledb psql -U analyticsuser -d analyticsdb -t -c "SELECT COUNT(*) FROM top_salesman_country;" 2>/dev/null | tr -d ' ')
if [ "$SALESMAN_COUNT" -gt 0 ]; then
    pass "Salesman aggregations exist in TimescaleDB (count: $SALESMAN_COUNT)"
else
    fail "No salesman aggregations found in TimescaleDB"
fi

echo ""
echo "Test 6: Verify Query API endpoints"
echo "-------------------------------------------"

API_RESPONSE=$(curl -sf "http://localhost:8090/api/v1/sales/by-city?size=5" 2>/dev/null)
if [ $? -eq 0 ] && echo "$API_RESPONSE" | grep -q '"content"'; then
    RESULT_COUNT=$(echo "$API_RESPONSE" | grep -o '"totalElements":[0-9]*' | cut -d':' -f2)
    pass "Query API /by-city endpoint working (total elements: $RESULT_COUNT)"
else
    fail "Query API /by-city endpoint NOT working"
fi

API_RESPONSE=$(curl -sf "http://localhost:8090/api/v1/sales/top-salesman?limit=5" 2>/dev/null)
if [ $? -eq 0 ] && echo "$API_RESPONSE" | grep -q '"content"'; then
    RESULT_COUNT=$(echo "$API_RESPONSE" | grep -o '"totalElements":[0-9]*' | cut -d':' -f2)
    pass "Query API /top-salesman endpoint working (total elements: $RESULT_COUNT)"
else
    fail "Query API /top-salesman endpoint NOT working"
fi

echo ""
echo "Test 7: Verify sample data quality"
echo "-------------------------------------------"

TOP_CITY=$(podman exec timescaledb psql -U analyticsuser -d analyticsdb -t -c "SELECT city FROM top_sales_by_city ORDER BY total_sales DESC LIMIT 1;" 2>/dev/null | tr -d ' ')
if [ -n "$TOP_CITY" ]; then
    pass "Top city by sales: $TOP_CITY"
else
    fail "Could not retrieve top city"
fi

TOP_SALESMAN=$(podman exec timescaledb psql -U analyticsuser -d analyticsdb -t -c "SELECT salesman_name FROM top_salesman_country ORDER BY total_sales DESC LIMIT 1;" 2>/dev/null | tr -d ' ')
if [ -n "$TOP_SALESMAN" ]; then
    pass "Top salesman: $TOP_SALESMAN"
else
    fail "Could not retrieve top salesman"
fi

echo ""
echo "========================================"
echo "Test Results"
echo "========================================"
echo "Passed: $PASSED"
echo "Failed: $FAILED"
echo ""

if [ "$FAILED" -eq 0 ]; then
    echo "All tests passed successfully!"
    exit 0
else
    echo "Some tests failed. Check the output above for details."
    exit 1
fi
