#!/bin/sh

set -e

# Load environment variables from .env file
if [ -f .env ]; then
    . ./.env
else
    echo "Error: .env file not found"
    exit 1
fi

API_TOKEN="${OPENAI_API_TOKEN}"

curl -X POST https://api.openai.com/v1/responses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${API_TOKEN}" \
  -d '{"model": "o4-mini", "input": "What is Scala programming language?"}'
