#!/bin/bash

# Script to run tests with automatic Redis and Memcached setup and teardown

set -e

REDIS_CONTAINER_NAME="redis-test-bitonic"
MEMCACHED_CONTAINER_NAME="memcached-test-bitonic"
REDIS_PORT=6379
MEMCACHED_PORT=11211

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

cleanup() {
    echo -e "${YELLOW}Cleaning up containers...${NC}"
    podman stop $REDIS_CONTAINER_NAME 2>/dev/null || true
    podman rm $REDIS_CONTAINER_NAME 2>/dev/null || true
    podman stop $MEMCACHED_CONTAINER_NAME 2>/dev/null || true
    podman rm $MEMCACHED_CONTAINER_NAME 2>/dev/null || true
    echo -e "${GREEN}Cleanup complete${NC}"
}

trap cleanup EXIT

# Start Redis
echo -e "${YELLOW}Starting Redis container...${NC}"
podman stop $REDIS_CONTAINER_NAME 2>/dev/null || true
podman rm $REDIS_CONTAINER_NAME 2>/dev/null || true
podman run -d --name $REDIS_CONTAINER_NAME -p $REDIS_PORT:6379 redis:latest
echo -e "${GREEN}Redis container started${NC}"

# Start Memcached
echo -e "${YELLOW}Starting Memcached container...${NC}"
podman stop $MEMCACHED_CONTAINER_NAME 2>/dev/null || true
podman rm $MEMCACHED_CONTAINER_NAME 2>/dev/null || true
podman run -d --name $MEMCACHED_CONTAINER_NAME -p $MEMCACHED_PORT:11211 memcached:latest
echo -e "${GREEN}Memcached container started${NC}"

# Wait for Redis
echo -e "${YELLOW}Waiting for Redis to be ready...${NC}"
sleep 2
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

# Wait for Memcached
echo -e "${YELLOW}Waiting for Memcached to be ready...${NC}"
sleep 2
for i in {1..10}; do
    if echo "stats" | nc -w 1 localhost $MEMCACHED_PORT > /dev/null 2>&1; then
        echo -e "${GREEN}Memcached is ready!${NC}"
        break
    fi
    if [ $i -eq 10 ]; then
        echo -e "${RED}Memcached failed to start${NC}"
        exit 1
    fi
    sleep 1
done

# Run tests
echo -e "${YELLOW}Running tests...${NC}"
sbt test

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Tests failed${NC}"
    exit 1
fi
