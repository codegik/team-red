#!/bin/bash

# Script to run tests using docker-compose infrastructure

set -e

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

cleanup() {
    echo -e "${YELLOW}Shutting down infrastructure...${NC}"
    docker-compose down -v
    echo -e "${GREEN}Cleanup complete${NC}"
}

trap cleanup EXIT

# Start infrastructure
echo -e "${YELLOW}Starting infrastructure with docker-compose...${NC}"
docker-compose up -d redis memcached

# Wait for services to be healthy
echo -e "${YELLOW}Waiting for services to be healthy...${NC}"
for i in {1..30}; do
    REDIS_HEALTH=$(docker inspect --format='{{.State.Health.Status}}' redis 2>/dev/null || echo "starting")
    MEMCACHED_HEALTH=$(docker inspect --format='{{.State.Health.Status}}' memcached 2>/dev/null || echo "starting")
    
    if [ "$REDIS_HEALTH" == "healthy" ] && [ "$MEMCACHED_HEALTH" == "healthy" ]; then
        echo -e "${GREEN}All services are healthy!${NC}"
        break
    fi
    
    if [ $i -eq 30 ]; then
        echo -e "${RED}Services failed to become healthy${NC}"
        docker-compose logs
        exit 1
    fi
    
    echo -e "${YELLOW}Redis: $REDIS_HEALTH | Memcached: $MEMCACHED_HEALTH${NC}"
    sleep 2
done

# Run tests locally
echo -e "${YELLOW}Running tests...${NC}"
sbt test

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Tests failed${NC}"
    exit 1
fi
