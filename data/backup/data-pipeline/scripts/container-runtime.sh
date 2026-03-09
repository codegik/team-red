#!/bin/bash

set -e

detect_container_runtime() {
    if command -v podman >/dev/null 2>&1; then
        RUNTIME="podman"
        COMPOSE="podman compose"
    elif command -v docker >/dev/null 2>&1; then
        RUNTIME="docker"
        COMPOSE="docker compose"
    else
        echo "Error: Neither podman nor docker is installed."
        exit 1
    fi
}

detect_container_runtime
