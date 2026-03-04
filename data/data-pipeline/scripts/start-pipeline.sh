#!/bin/bash

set -e

echo "Starting Kappa Pipeline..."

cd "$(dirname "$0")/.."

source ./scripts/container-runtime.sh

# Build Java artifacts required by Dockerfiles unless SKIP_BUILD=1 is set.
if [ "${SKIP_BUILD:-0}" != "1" ]; then
    echo "Building Java artifacts (Maven)..."
    if command -v mvn >/dev/null 2>&1; then
        # Use parallel build with tests skipped to speed up local runs
        mvn -DskipTests -T1C package
    else
        echo "Maven (mvn) not found in PATH."
        echo "Either install Maven or set SKIP_BUILD=1 to skip building artifacts (only recommended if artifacts already exist)."
        exit 1
    fi
else
    echo "SKIP_BUILD=1 set; skipping Maven build. Make sure required JARs are present under target/ for each module."
fi

$COMPOSE up -d

echo "Waiting for services to be ready..."
./scripts/wait-for-services.sh

echo "Creating Kafka topics..."
./scripts/create-topics.sh

echo "Restarting stream processors to ensure topic metadata is current..."
sleep 2
$RUNTIME restart stream-processor-city stream-processor-salesman

echo "Waiting for stream processors to stabilize..."
sleep 10

echo "Checking stream processor status..."
if $RUNTIME ps --filter "name=stream-processor-city" --format "{{.Names}}" | grep -q stream-processor-city; then
    echo "  stream-processor-city: Running"
else
    echo "  stream-processor-city: NOT Running"
fi

if $RUNTIME ps --filter "name=stream-processor-salesman" --format "{{.Names}}" | grep -q stream-processor-salesman; then
    echo "  stream-processor-salesman: Running"
else
    echo "  stream-processor-salesman: NOT Running"
fi

echo ""
echo "Pipeline is ready!"
echo ""
echo "Access points:"
echo "  - Query API: http://localhost:8090"
echo "  - API Docs: http://localhost:8090/swagger-ui.html"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - Prometheus: http://localhost:9090"
echo ""
echo "Generate test data:"
echo "  ./scripts/generate-db-sales.sh 100"
echo "  ./scripts/generate-file-sales.sh 50"
echo ""
echo "Run end-to-end tests:"
echo "  ./scripts/run-tests.sh"
