#!/bin/bash

set -e

echo "Starting Kappa Pipeline..."

cd "$(dirname "$0")/.."

podman compose up -d

echo "Waiting for services to be ready..."
./scripts/wait-for-services.sh

echo "Creating Kafka topics..."
./scripts/create-topics.sh

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
