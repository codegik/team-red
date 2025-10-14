#!/bin/bash

# Clean Docker/Podman Container Volumes Script
# This script stops containers, removes them, and cleans up volumes and networks.

set -e  # Exit on any error

echo "🧹 Starting container and volume cleanup..."

# Function to detect container runtime (Docker or Podman)
detect_runtime() {
    if command -v podman &> /dev/null; then
        echo "podman"
    elif command -v docker &> /dev/null; then
        echo "docker"
    else
        echo "Error: Neither Docker nor Podman is installed or in PATH"
        exit 1
    fi
}

RUNTIME=$(detect_runtime)
echo "📦 Using container runtime: $RUNTIME"

# Stop and remove containers from docker-compose
echo "🛑 Stopping containers..."
$RUNTIME compose down

# Remove all containers (including stopped ones)
echo "🗑️  Removing all stopped containers..."
$RUNTIME container prune -f

# Remove the specific Jenkins volume if it exists
echo "🔧 Removing Jenkins volume..."
$RUNTIME volume rm team-red-hackathon-2025_jenkins_home 2>/dev/null || echo "   Jenkins volume not found (already removed)"

# Remove all unused networks
echo "🌐 Removing Jenkins networks..."
$RUNTIME network rm team-red-hackathon-2025_default 2>/dev/null || echo "   Jenkins network not found (already removed)"

echo "✅ Cleanup completed!"
echo ""
echo "To rebuild and start Jenkins:"
echo "  $RUNTIME compose up -d"
echo ""
echo "To see remaining volumes:"
echo "  $RUNTIME volume ls"