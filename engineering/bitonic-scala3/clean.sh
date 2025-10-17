#!/bin/bash

set -e

echo "🧹 Stopping and removing all containers, volumes, and network from Docker Compose..."

# Step 1: Stop and remove all containers, networks, volumes defined in docker-compose
docker compose down --volumes --remove-orphans

# Step 2: Remove the custom 'bitonic' network if still present
NETWORK_NAME="bitonic"
if docker network ls --format '{{.Name}}' | grep -q "^${NETWORK_NAME}$"; then
  echo "🔌 Removing network '${NETWORK_NAME}'..."
  docker network rm "$NETWORK_NAME"
fi

echo "✅ Clean-up complete."
