#!/bin/bash

set -e

echo "Creating Kafka topics..."

create_topic() {
    local topic=$1
    local partitions=${2:-3}
    local replication=${3:-1}

    docker exec kafka kafka-topics --bootstrap-server localhost:9092 \
        --create \
        --if-not-exists \
        --topic "$topic" \
        --partitions "$partitions" \
        --replication-factor "$replication"

    echo "Topic created: $topic"
}

create_topic "sales.raw.db" 3 1
create_topic "sales.raw.file" 3 1
create_topic "sales.raw.soap" 3 1

echo "All topics created successfully!"
