#!/bin/bash

set -e

echo "Stopping Kappa Pipeline..."

cd "$(dirname "$0")/.."

source ./scripts/container-runtime.sh

$COMPOSE down

echo "Pipeline stopped successfully!"
