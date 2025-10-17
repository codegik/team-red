#!/bin/bash

# Simple script to run unit tests only (without Redis setup)
# For fast feedback during development

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Running unit tests...${NC}"

# Run tests with testOnly to run specific test patterns if needed
sbt "testOnly *Test"

# Check test result
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Unit tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Unit tests failed${NC}"
    exit 1
fi
