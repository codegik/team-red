#!/bin/bash

set -e

cd "$(dirname "$0")/.."

REMOVE_VOLUMES=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--volumes)
            REMOVE_VOLUMES=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [-v|--volumes]"
            echo "  -v, --volumes    Remove all volumes (data will be lost)"
            exit 1
            ;;
    esac
done

echo "Stopping Kappa Pipeline..."
echo ""

if [ "$REMOVE_VOLUMES" = true ]; then
    echo "Stopping services and removing volumes..."
    echo "WARNING: All data will be deleted!"
    podman compose down -v
    echo "Services stopped and volumes removed."
else
    echo "Stopping services (preserving volumes)..."
    podman compose down
    echo "Services stopped (data preserved)."
fi

echo ""
echo "Cleanup complete!"
echo ""

if [ "$REMOVE_VOLUMES" = false ]; then
    echo "To remove all data volumes, run:"
    echo "  ./scripts/cleanup.sh --volumes"
fi
