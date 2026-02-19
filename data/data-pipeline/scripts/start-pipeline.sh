#!/bin/bash

set -e

echo "Starting Kappa Pipeline..."

cd "$(dirname "$0")/.."

podman compose up -d

echo "Waiting for services to be ready..."
./scripts/wait-for-services.sh

echo "Creating Kafka topics..."
./scripts/create-topics.sh

echo "Restarting stream processors to ensure topic metadata is current..."
sleep 2
podman restart stream-processor-city stream-processor-salesman

echo "Waiting for stream processors to stabilize..."
sleep 10

echo "Checking stream processor status..."
if podman ps --filter "name=stream-processor-city" --format "{{.Names}}" | grep -q stream-processor-city; then
    echo "  stream-processor-city: Running"
else
    echo "  stream-processor-city: NOT Running"
fi

if podman ps --filter "name=stream-processor-salesman" --format "{{.Names}}" | grep -q stream-processor-salesman; then
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
