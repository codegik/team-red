#!/bin/bash

# Kappa Pipeline Quick Start Script
# This script helps you quickly start and verify the data pipeline

set -e

echo "🚀 Starting Kappa Architecture Data Pipeline..."
echo "=================================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose not found. Please install Docker Compose first."
    exit 1
fi

echo "📦 Starting all services with docker-compose..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to be healthy (this may take 2-3 minutes)..."
sleep 30

echo ""
echo "🔍 Checking service health..."
echo ""

# Check each service
services=("kappa-zookeeper" "kappa-kafka" "kappa-postgres-source" "kappa-postgres-results" "kappa-prometheus" "kappa-grafana")

for service in "${services[@]}"; do
    if docker ps --format '{{.Names}}' | grep -q "^${service}$"; then
        echo -e "${GREEN}✓${NC} $service is running"
    else
        echo -e "${YELLOW}⚠${NC} $service is not running yet"
    fi
done

echo ""
echo "⏳ Waiting additional 60 seconds for application services to start..."
sleep 60

echo ""
echo "📊 Service URLs:"
echo "=================================================="
echo "REST API (Swagger):     http://localhost:8082/swagger-ui.html"
echo "Data Generators:        http://localhost:8080/actuator/health"
echo "Stream Processors:      http://localhost:8081/actuator/health"
echo "Grafana Dashboard:      http://localhost:3000 (admin/admin)"
echo "Prometheus:             http://localhost:9090"
echo "Kafka Connect:          http://localhost:8083"
echo ""

echo "📡 Testing API Endpoints..."
echo "=================================================="
echo ""

# Test top sales by city
echo "🏙️  Testing: Top Sales by City"
if curl -s -f http://localhost:8082/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} API is healthy"
    echo "   Endpoint: GET http://localhost:8082/api/v1/top-sales/by-city?limit=10"
else
    echo -e "${YELLOW}⚠${NC} API not ready yet, please wait and try: curl http://localhost:8082/api/v1/top-sales/by-city?limit=10"
fi

echo ""
echo "👤 Testing: Top Salesman by Country"
echo "   Endpoint: GET http://localhost:8082/api/v1/top-salesmen/by-country?limit=10"

echo ""
echo "=================================================="
echo "🎉 Pipeline Started Successfully!"
echo "=================================================="
echo ""
echo "📝 Next Steps:"
echo "1. Wait 2-3 minutes for data to flow through the pipeline"
echo "2. Open Swagger UI: http://localhost:8082/swagger-ui.html"
echo "3. Try the demo commands below:"
echo ""
echo "Demo Command 1 - Top Sales by City:"
echo "  curl http://localhost:8082/api/v1/top-sales/by-city?limit=5 | jq"
echo ""
echo "Demo Command 2 - Top Salesman by Country:"
echo "  curl http://localhost:8082/api/v1/top-salesmen/by-country?country=Brazil&limit=5 | jq"
echo ""
echo "Watch real-time updates:"
echo "  watch -n 2 'curl -s http://localhost:8082/api/v1/top-sales/by-city?limit=5 | jq'"
echo ""
echo "View logs:"
echo "  docker-compose logs -f stream-processors"
echo ""
echo "Stop all services:"
echo "  docker-compose down"
echo ""

