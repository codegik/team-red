#!/bin/bash

set -e

COUNT=${1:-50}

echo "Generating $COUNT sales records as CSV file..."

CITIES=("New York" "San Francisco" "Los Angeles" "Chicago" "Boston" "Seattle" "Miami" "Austin" "Denver" "Portland")
PRODUCTS=("Laptop" "Mouse" "Keyboard" "Monitor" "Webcam" "Headset" "Tablet" "Printer" "Scanner" "Router")
SALESMEN=("John Doe" "Jane Smith" "Bob Johnson" "Alice Williams" "Charlie Brown")

FILENAME="data/input/sales-$(date +%s).csv"

echo "sale_id,timestamp,salesman_id,salesman_name,customer_id,product_id,product_name,quantity,unit_price,total_amount,city,country" > "$FILENAME"

for i in $(seq 1 $COUNT); do
    SALE_ID="file-sale-$(date +%s)-$i"
    TIMESTAMP=$(date +%s)000
    SALESMAN_IDX=$((RANDOM % ${#SALESMEN[@]}))
    SALESMAN_NAME="${SALESMEN[$SALESMAN_IDX]}"
    SALESMAN_ID="sm-$(echo $SALESMAN_NAME | tr ' ' '-' | tr '[:upper:]' '[:lower:]')"
    CUSTOMER_ID="cust-$((RANDOM % 1000))"
    PRODUCT_IDX=$((RANDOM % ${#PRODUCTS[@]}))
    PRODUCT_NAME="${PRODUCTS[$PRODUCT_IDX]}"
    PRODUCT_ID="prod-$(echo $PRODUCT_NAME | tr '[:upper:]' '[:lower:]')"
    QUANTITY=$((RANDOM % 10 + 1))
    UNIT_PRICE=$(awk -v min=50 -v max=1500 'BEGIN{srand(); print min+rand()*(max-min)}')
    TOTAL_AMOUNT=$(awk "BEGIN {print $QUANTITY * $UNIT_PRICE}")
    CITY_IDX=$((RANDOM % ${#CITIES[@]}))
    CITY="${CITIES[$CITY_IDX]}"

    echo "$SALE_ID,$TIMESTAMP,$SALESMAN_ID,$SALESMAN_NAME,$CUSTOMER_ID,$PRODUCT_ID,$PRODUCT_NAME,$QUANTITY,$UNIT_PRICE,$TOTAL_AMOUNT,$CITY,USA" >> "$FILENAME"
done

echo "Successfully generated CSV file: $FILENAME"
