#!/bin/bash

set -e

COUNT=${1:-100}

CITIES=("New York" "San Francisco" "Los Angeles" "Chicago" "Boston" "Seattle" "Austin" "Miami" "Denver" "Atlanta")
PRODUCTS=("Laptop" "Mouse" "Keyboard" "Monitor" "Headphones" "Webcam" "Desk" "Chair" "USB Cable" "Hard Drive")
SALESMEN=("John Doe" "Jane Smith" "Bob Johnson" "Alice Williams" "Charlie Brown" "Diana Davis" "Eve Miller" "Frank Wilson" "Grace Lee" "Henry Taylor")

PGPASSWORD=sourcepass

for i in $(seq 1 $COUNT); do
    SALE_ID="sale-$(uuidgen)"
    TIMESTAMP=$(($(date +%s) * 1000 + RANDOM % 1000))
    SALESMAN_ID="sm-$(printf '%03d' $((RANDOM % 10 + 1)))"
    SALESMAN_NAME="${SALESMEN[$((RANDOM % 10))]}"
    CUSTOMER_ID="cust-$(printf '%05d' $((RANDOM % 10000)))"
    PRODUCT_ID="prod-$(printf '%03d' $((RANDOM % 10 + 1)))"
    PRODUCT_NAME="${PRODUCTS[$((RANDOM % 10))]}"
    QUANTITY=$((RANDOM % 10 + 1))
    UNIT_PRICE=$(awk -v min=10 -v max=2000 'BEGIN{srand(); print min+rand()*(max-min)}')
    TOTAL_AMOUNT=$(awk -v q=$QUANTITY -v p=$UNIT_PRICE 'BEGIN{print q*p}')
    CITY="${CITIES[$((RANDOM % 10))]}"
    COUNTRY="USA"

    podman exec postgres-source psql -U sourceuser -d sourcedb -c \
        "INSERT INTO sales (sale_id, timestamp, salesman_id, salesman_name, customer_id, product_id, product_name, quantity, unit_price, total_amount, city, country) VALUES ('$SALE_ID', $TIMESTAMP, '$SALESMAN_ID', '$SALESMAN_NAME', '$CUSTOMER_ID', '$PRODUCT_ID', '$PRODUCT_NAME', $QUANTITY, $UNIT_PRICE, $TOTAL_AMOUNT, '$CITY', '$COUNTRY');"

    if [ $((i % 100)) -eq 0 ]; then
        echo "Inserted $i records"
    fi
done

echo "Successfully inserted $COUNT sales records into PostgreSQL"
