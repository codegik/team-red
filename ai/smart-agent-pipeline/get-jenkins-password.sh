#!/bin/bash

set -e  # Exit on any error

echo "Getting jenkins password..."

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

PASS=$($RUNTIME exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword)
echo "Jenkins initial admin password: $PASS"
echo ""
