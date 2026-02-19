#!/bin/bash

set -e

echo "Waiting for services to be ready..."

check_service() {
    local name=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -sf "$url" > /dev/null 2>&1; then
            echo "$name is ready"
            return 0
        fi
        echo "Waiting for $name... (attempt $attempt/$max_attempts)"
        sleep 1
        attempt=$((attempt + 1))
    done

    echo "$name failed to start"
    return 1
}

check_kafka() {
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if podman exec kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; then
            echo "Kafka is ready"
            return 0
        fi
        echo "Waiting for Kafka... (attempt $attempt/$max_attempts)"
        sleep 1
        attempt=$((attempt + 1))
    done

    echo "Kafka failed to start"
    return 1
}

check_postgres() {
    local container=$1
    local user=$2
    local db=$3
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if podman exec $container psql -U $user -d $db -c "SELECT 1" > /dev/null 2>&1; then
            echo "$container is ready"
            return 0
        fi
        echo "Waiting for $container... (attempt $attempt/$max_attempts)"
        sleep 1
        attempt=$((attempt + 1))
    done

    echo "$container failed to start"
    return 1
}

check_postgres postgres-source sourceuser sourcedb
check_postgres timescaledb analyticsuser analyticsdb
check_kafka
check_service "Schema Registry" "http://localhost:8081"
check_service "Prometheus" "http://localhost:9090/-/healthy"
check_service "Grafana" "http://localhost:3000/api/health"

echo "All services are ready!"
