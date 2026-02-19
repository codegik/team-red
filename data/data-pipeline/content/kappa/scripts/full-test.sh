#!/bin/bash

set -e

cd "$(dirname "$0")/.."

CLEANUP_AFTER=true

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-cleanup)
            CLEANUP_AFTER=false
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--no-cleanup]"
            echo "  --no-cleanup    Leave services running after tests"
            exit 1
            ;;
    esac
done

echo "========================================"
echo "Kappa Pipeline Full Test Suite"
echo "========================================"
echo ""

cleanup_on_exit() {
    if [ "$CLEANUP_AFTER" = true ]; then
        echo ""
        echo "Cleaning up..."
        ./scripts/cleanup.sh --volumes
    else
        echo ""
        echo "Services left running (use --no-cleanup to change)"
        echo "To stop services: ./scripts/cleanup.sh"
    fi
}

trap cleanup_on_exit EXIT

echo "Step 1: Build all services"
echo "-------------------------------------------"
./scripts/build-all.sh
echo ""

echo "Step 2: Clean up any existing deployment"
echo "-------------------------------------------"
./scripts/cleanup.sh --volumes || true
echo ""

echo "Step 3: Start the pipeline"
echo "-------------------------------------------"
./scripts/start-pipeline.sh
echo ""

echo "Step 4: Run end-to-end tests"
echo "-------------------------------------------"
./scripts/run-tests.sh
TEST_RESULT=$?

echo ""
echo "========================================"
echo "Full Test Suite Complete"
echo "========================================"

if [ $TEST_RESULT -eq 0 ]; then
    echo "Status: SUCCESS"
else
    echo "Status: FAILED"
fi

exit $TEST_RESULT
