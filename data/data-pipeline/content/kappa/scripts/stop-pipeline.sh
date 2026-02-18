#!/bin/bash

set -e

echo "Stopping Kappa Pipeline..."

cd "$(dirname "$0")/.."

podman compose down

echo "Pipeline stopped successfully!"
