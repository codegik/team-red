#!/bin/bash

set -e

echo "Stopping Kappa Pipeline..."

cd "$(dirname "$0")/.."

docker-compose down

echo "Pipeline stopped successfully!"
