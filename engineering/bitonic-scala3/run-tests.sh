#!/bin/bash

# Script to run tests with automatic Redis setup and teardown
# This script starts Redis, runs tests, and cleans up regardless of test outcome

set -e  # Exit on error (but we'll handle cleanup in trap)

REDIS_CONTAINER_NAME="redis-test-bitonic"
REDIS_PORT=6379

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to cleanup Redis container
cleanup() {
    echo -e "${YELLOW}Cleaning up Redis container...${NC}"
    podman stop $REDIS_CONTAINER_NAME 2>/dev/null || true
    podman rm $REDIS_CONTAINER_NAME 2>/dev/null || true
    echo -e "${GREEN}Cleanup complete${NC}"
}

# Trap EXIT to ensure cleanup always runs
trap cleanup EXIT

echo -e "${YELLOW}Starting Redis container...${NC}"

# Stop and remove any existing container with the same name
podman stop $REDIS_CONTAINER_NAME 2>/dev/null || true
podman rm $REDIS_CONTAINER_NAME 2>/dev/null || true

# Start Redis container
podman run -d \
    --name $REDIS_CONTAINER_NAME \
    -p $REDIS_PORT:6379 \
    redis:latest

echo -e "${GREEN}Redis container started${NC}"

# Wait for Redis to be ready
echo -e "${YELLOW}Waiting for Redis to be ready...${NC}"
sleep 2

# Check if Redis is responding
for i in {1..10}; do
    if podman exec $REDIS_CONTAINER_NAME redis-cli ping 2>/dev/null | grep -q "PONG"; then
        echo -e "${GREEN}Redis is ready!${NC}"
        break
    fi
    if [ $i -eq 10 ]; then
        echo -e "${RED}Redis failed to start${NC}"
        exit 1
    fi
    sleep 1
done

# Run the tests
echo -e "${YELLOW}Running tests...${NC}"
sbt test

# Check test result
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Tests failed${NC}"
    exit 1
fi

