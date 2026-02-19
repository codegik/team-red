#!/bin/bash

set -e

COUNT=${1:-100}

CITIES=("New York" "San Francisco" "Los Angeles" "Chicago" "Boston" "Seattle" "Austin" "Miami" "Denver" "Atlanta")
PRODUCTS=("Laptop" "Mouse" "Keyboard" "Monitor" "Headphones" "Webcam" "Desk" "Chair" "USB Cable" "Hard Drive")
SALESMEN=("John Doe" "Jane Smith" "Bob Johnson" "Alice Williams" "Charlie Brown" "Diana Davis" "Eve Miller" "Frank Wilson" "Grace Lee" "Henry Taylor")

DATA_DIR="./data/input"
mkdir -p $DATA_DIR

for i in $(seq 1 $COUNT); do
    SALE_ID="sale-file-$(uuidgen)"
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

    if [ $((i % 2)) -eq 0 ]; then
        FILE="$DATA_DIR/sales-$TIMESTAMP-$RANDOM.csv"
        echo "sale_id,timestamp,salesman_id,salesman_name,customer_id,product_id,product_name,quantity,unit_price,total_amount,city,country" > $FILE
        echo "$SALE_ID,$TIMESTAMP,$SALESMAN_ID,$SALESMAN_NAME,$CUSTOMER_ID,$PRODUCT_ID,$PRODUCT_NAME,$QUANTITY,$UNIT_PRICE,$TOTAL_AMOUNT,$CITY,$COUNTRY" >> $FILE
    else
        FILE="$DATA_DIR/sales-$TIMESTAMP-$RANDOM.json"
        cat > $FILE <<INNER_EOF
{
  "saleId": "$SALE_ID",
  "timestamp": $TIMESTAMP,
  "salesmanId": "$SALESMAN_ID",
  "salesmanName": "$SALESMAN_NAME",
  "customerId": "$CUSTOMER_ID",
  "productId": "$PRODUCT_ID",
  "productName": "$PRODUCT_NAME",
  "quantity": $QUANTITY,
  "unitPrice": $UNIT_PRICE,
  "totalAmount": $TOTAL_AMOUNT,
  "city": "$CITY",
  "country": "$COUNTRY"
}
INNER_EOF
    fi

    if [ $((i % 100)) -eq 0 ]; then
        echo "Generated $i files"
    fi
done

echo "Successfully generated $COUNT sales files (CSV and JSON)"
