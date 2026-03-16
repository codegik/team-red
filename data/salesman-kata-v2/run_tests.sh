#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EXPECTED_JAVA_MAJOR="25"
MODE="${1:-auto}"
DOCKER_TEST_IMAGE="${DOCKER_TEST_IMAGE:-maven:3.9-eclipse-temurin-25}"
SERVICES=(
  "services/csv-connector-source"
  "services/postgres-connector-source"
  "services/postgres-enricher"
  "services/sales-aggregator"
  "services/sales-consumer"
  "services/soap-connector-source"
)

usage() {
  cat <<EOF
Usage: ./run_tests.sh [auto|local|docker]

Modes:
  auto   Use local Java 25 if available, otherwise run tests in Docker
  local  Require local Java 25 and run mvn test on host
  docker Run mvn test inside Docker using $DOCKER_TEST_IMAGE
EOF
}

init_sdkman() {
  if [[ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
    local had_nounset=0
    case $- in
      *u*) had_nounset=1 ;;
    esac

    if [[ $had_nounset -eq 1 ]]; then
      set +u
    fi

    # shellcheck source=/dev/null
    source "$HOME/.sdkman/bin/sdkman-init.sh"

    if [[ $had_nounset -eq 1 ]]; then
      set -u
    fi
  fi
}

current_java_major() {
  local version_output
  version_output="$(java -version 2>&1 | head -n 1 || true)"
  sed -E 's/.*version "([0-9]+).*/\1/' <<<"$version_output"
}

ensure_local_java_25() {
  init_sdkman

  local version_output java_major
  version_output="$(java -version 2>&1 | head -n 1)"
  java_major="$(current_java_major)"

  if [[ "$java_major" != "$EXPECTED_JAVA_MAJOR" ]]; then
    cat <<EOF
Expected Java $EXPECTED_JAVA_MAJOR, but found:
$version_output

If you use SDKMAN, run:
  source "$HOME/.sdkman/bin/sdkman-init.sh"
  sdk use java 25.0.2-tem

Or use Docker mode:
  ./run_tests.sh docker
EOF
    exit 1
  fi

  echo "Using local Java $java_major"
}

ensure_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is not installed or not in PATH."
    exit 1
  fi
}

run_local_tests() {
  ensure_local_java_25

  for service in "${SERVICES[@]}"; do
    echo
    echo "==> Running unit tests locally in $service"
    (
      cd "$ROOT_DIR/$service"
      mvn -q test
    )
  done
}

run_docker_tests() {
  ensure_docker
  echo "Using Docker image $DOCKER_TEST_IMAGE"

  for service in "${SERVICES[@]}"; do
    echo
    echo "==> Running unit tests in Docker for $service"
    docker run --rm \
      -v "$ROOT_DIR/$service:/app" \
      -v "$HOME/.m2:/root/.m2" \
      -w /app \
      "$DOCKER_TEST_IMAGE" \
      mvn -q test
  done
}

case "$MODE" in
  auto)
    if [[ "$(current_java_major)" == "$EXPECTED_JAVA_MAJOR" ]]; then
      run_local_tests
    else
      run_docker_tests
    fi
    ;;
  local)
    run_local_tests
    ;;
  docker)
    run_docker_tests
    ;;
  -h|--help|help)
    usage
    exit 0
    ;;
  *)
    echo "Unknown mode: $MODE"
    echo
    usage
    exit 1
    ;;
esac

echo
echo "All unit tests passed."
