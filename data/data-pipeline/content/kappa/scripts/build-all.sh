#!/bin/bash

set -e

echo "Building all services..."

cd "$(dirname "$0")/.."

mvn clean package -DskipTests

echo "Build completed successfully!"
echo "Docker images can be built with: docker-compose build"
