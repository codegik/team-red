#!/bin/sh
set -e

REGISTRY_URL=${SCHEMA_REGISTRY_URL:-http://schema-registry:8081}

echo "Waiting for Schema Registry at $REGISTRY_URL ..."
until curl -sf "$REGISTRY_URL/subjects" > /dev/null; do
  sleep 2
done
echo "Schema Registry is up."

register() {
  local subject=$1
  local file=$2

  echo "Registering subject: $subject"

  # Compact the schema and JSON-stringify it for embedding in the request body
  schema=$(jq -c . "$file" | jq -Rs .)

  http_code=$(curl -s -o /tmp/sr_response.json -w "%{http_code}" \
    -X POST "$REGISTRY_URL/subjects/$subject/versions" \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    -d "{\"schemaType\": \"JSON\", \"schema\": $schema}")

  if [ "$http_code" = "200" ]; then
    echo "  Registered (id=$(cat /tmp/sr_response.json | jq .id))"
  elif [ "$http_code" = "409" ]; then
    echo "  Already exists (schema compatible, skipping)"
  else
    echo "  ERROR HTTP $http_code: $(cat /tmp/sr_response.json)"
    exit 1
  fi
}

register "raw_csv-value"      "/schemas/raw-csv-value.json"
register "raw_soap-value"     "/schemas/raw-soap-value.json"
register "raw_postgres-value" "/schemas/raw-postgres-value.json"
register "sales-value"        "/schemas/sales-value.json"

echo "All schemas registered successfully."
