# TOP Salesman - Data KATA

## Problem Context

We are building a software for a Eletronic Store. This store sales cell phones, computers, and relateds.

**Company characteristics:**

- Multi warehouse
- Multi sales person
- Multi retailers

### Problem to solve

Every Monday, the CEO asks:

- "Which cities are generating the most revenue this month?"
- "Who are our top-performing salespeople in each country?"

**Current solution:**

Monday 8:00 AM - CEO asks for the report

Data Analyst Maria does:

1. Export data from PostgreSQL → Excel (2 hours)
2. Download CSV files from shared folder (30 min)
3. Manually poll SOAP service for sales data (3 hours)
4. Merge everything in Excel (4 hours)
5. Create pivot tables and charts (2 hours)
6. Send email to CEO (30 min)

Wednesday 6:00 PM - Report finally delivered

## Knowledge base

### 1) What is Data Ingestion?

Ingestion is the process of collection and importing data from various sources into your pipeline system for processing. It's a funnel that receives data from many places

```
[Relational DB] ──────┐
                      │
[File System]  ───────┼──▶  [ Your Pipeline ]  ──▶  [Processing]
                      │
[SOAP/WS-*]    ───────┘
```

### 2) What is Data Lineage?

Data Lineage tracks the origin, movement, and transformation of data throughout its lifecycle. It answers:

- Where did this data come from? (origin)
- What happened to it? (transformations)
- Where did it go? (destination)

```
[Raw Sales Data] ──▶ [Cleaned] ──▶ [Aggregated by City] ──▶ [Final DB]
      │                  │                  │                    │
      └──────────────────┴──────────────────┴────────────────────┘
                    Data Lineage Graph (tracked)
```

### 3) What is Message Broker

A Message Broker is middleware that translates and routes messages between different applications/services.

```
┌──────────┐                              ┌──────────┐
│ Producer │ ───▶  ┌────────────────┐ ───▶│ Consumer │
│ (Source) │       │ MESSAGE BROKER │     │ (Target) │
└──────────┘       │    (Kafka)     │     └──────────┘
                   └────────────────┘
                          │
                   • Receives messages
                   • Stores temporarily
                   • Routes to consumers
                   • Guarantees delivery
```

## Solution

### Data Sources

All three data sources share the same consistent master data: **22 products**, **15 salesmen**, and **18 stores** across Brazil.

#### Postgresql

Origin: Original ERP system (SAP on PostgreSQL)<br/>
Data: Real-time sales from POS terminals<br/>
Volume: ~50,000 transactions per day<br/>
Update freq: Real-time (continuous, every 5 seconds)<br/>
Database: `electromart`

Schema is initialized automatically via `init.sql` mounted into the PostgreSQL container (`/docker-entrypoint-initdb.d/`). The Node.js generator only inserts new sales.

**Tables:** `products`, `salesmen`, `stores`, `sales`

```sql
-- Sales table structure
sale_id     BIGSERIAL PRIMARY KEY
product_id  INTEGER  (FK → products)
salesman_id INTEGER  (FK → salesmen)
store_id    INTEGER  (FK → stores)
quantity    INTEGER
unit_price  DECIMAL(10,2)
total_amount DECIMAL(12,2)
status      VARCHAR(20)  -- PENDING | CONFIRMED | CANCELLED
sale_timestamp TIMESTAMP
```

<img src="./data-sources/postgresql/Postgresql.drawio.png" alt="postgresql"/>

#### CSV Files (via MinIO S3)

Origin: Acquired company's legacy system (2018)<br/>
Data: Daily sales export<br/>
Volume: 1-5 records per file<br/>
Update freq: Every 5 seconds<br/>
Storage: MinIO (S3-compatible object storage)<br/>
Bucket: `sales-csv`

**Architecture:**

```
[CSV Generator] ──▶ [MinIO S3 Bucket] ──webhook──▶ [CSV Connector] ──▶ [Kafka]
```

The CSV connector is event-driven: MinIO sends webhook notifications when new files are uploaded, eliminating polling and enabling true real-time streaming.

**MinIO Console:** http://localhost:9001 (minioadmin / minioadmin123)

```csv
sale_id,product_code,product_name,category,brand,salesman_name,salesman_email,region,store_name,city,store_type,quantity,unit_price,total_amount,status,sale_date
CSV2024011543210,IPHONE15PRO256,iPhone 15 Pro 256GB,SMARTPHONE,Apple,João Silva,joao.silva@electromart.com.br,São Paulo,Magazine Luiza Paulista,São Paulo,RETAIL,2,8999.00,17998.00,PENDING,2024-01-15 10:32:15
CSV2024011554321,GALAXYS24ULTRA,Galaxy S24 Ultra,SMARTPHONE,Samsung,Maria Oliveira,maria.oliveira@electromart.com.br,São Paulo,Fast Shop Morumbi,São Paulo,RETAIL,1,7999.00,7999.00,CONFIRMED,2024-01-15 11:05:42
CSV2024011565432,MACBOOKPRO14,MacBook Pro 14",LAPTOP,Apple,Pedro Santos,pedro.santos@electromart.com.br,Rio de Janeiro,Magazine Luiza Copacabana,Rio de Janeiro,RETAIL,1,18999.00,18999.00,CANCELLED,2024-01-15 14:22:08
```

#### SOAP

Origin: Legacy sales system (2012)<br/>
Data: Sales transactions (mocked/generated on-demand)<br/>
Volume: 1-5 records per request<br/>
Update freq: Polled every 5 seconds<br/>
Protocol: SOAP 1.1 / XML

```
URL: http://localhost:8080/sales
Method: POST
Content-Type: text/xml
```

**Request (cursor-based pagination)**

```xml
<cursor>SOAP-20240115-00050</cursor>
<pageSize>100</pageSize>
```

**Example SOAP Response**

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:sale="http://electromart.com/sales">
  <soapenv:Body>
    <sale:GetSalesResponse>
      <sale:totalRecords>3</sale:totalRecords>
      <sale:nextCursor>SOAP-20240115-00003</sale:nextCursor>
      <sale:hasMore>false</sale:hasMore>
      <sale:sales>
        <sale:record>
          <sale:saleId>SOAP-20240115-00001</sale:saleId>
          <sale:productCode>IPHONE15PRO256</sale:productCode>
          <sale:productName>iPhone 15 Pro 256GB</sale:productName>
          <sale:category>SMARTPHONE</sale:category>
          <sale:brand>Apple</sale:brand>
          <sale:salesmanName>João Silva</sale:salesmanName>
          <sale:salesmanEmail>joao.silva@electromart.com.br</sale:salesmanEmail>
          <sale:region>São Paulo</sale:region>
          <sale:storeName>Magazine Luiza Paulista</sale:storeName>
          <sale:city>São Paulo</sale:city>
          <sale:storeType>RETAIL</sale:storeType>
          <sale:quantity>2</sale:quantity>
          <sale:unitPrice>8999.00</sale:unitPrice>
          <sale:totalAmount>17998.00</sale:totalAmount>
          <sale:status>PENDING</sale:status>
          <sale:saleTimestamp>2024-01-15T10:32:15Z</sale:saleTimestamp>
        </sale:record>
        <sale:record>
          <sale:saleId>SOAP-20240115-00002</sale:saleId>
          <sale:productCode>GALAXYS24ULTRA</sale:productCode>
          <sale:productName>Galaxy S24 Ultra</sale:productName>
          <sale:category>SMARTPHONE</sale:category>
          <sale:brand>Samsung</sale:brand>
          <sale:salesmanName>Pedro Santos</sale:salesmanName>
          <sale:salesmanEmail>pedro.santos@electromart.com.br</sale:salesmanEmail>
          <sale:region>Rio de Janeiro</sale:region>
          <sale:storeName>Casas Bahia Madureira</sale:storeName>
          <sale:city>Rio de Janeiro</sale:city>
          <sale:storeType>RETAIL</sale:storeType>
          <sale:quantity>1</sale:quantity>
          <sale:unitPrice>7999.00</sale:unitPrice>
          <sale:totalAmount>7999.00</sale:totalAmount>
          <sale:status>CONFIRMED</sale:status>
          <sale:saleTimestamp>2024-01-15T11:05:42Z</sale:saleTimestamp>
        </sale:record>
      </sale:sales>
    </sale:GetSalesResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

## Services needed

| #   | Service/Component                | Purpose                                      | Implemented |
| --- | -------------------------------- | -------------------------------------------- | ----------- |
| 1   | PostgreSQL                       | Source 1 - Sales transactions from São Paulo | ✅ Yes      |
| 2   | CSV Files (Local Volume)         | Source 2 - Sales from Minas Gerais           | ✅ Yes      |
| 3   | SOAP Service                     | Source 3 - Legacy sales from Rio de Janeiro  | ✅ Yes      |
| 4   | Message Broker (Kafka)           | Event streaming backbone                     | ✅ Yes      |
| 5   | Kafka Connect (Debezium)         | CDC connector for PostgreSQL                 | ✅ Yes      |
| 6   | CSV Connector                    | Reads CSV files and publishes to Kafka       | ✅ Yes      |
| 7   | SOAP Connector                   | Polls SOAP service and publishes to Kafka    | ✅ Yes      |
| 8   | Postgres Connector               | Enriches PostgreSQL CDC events               | ✅ Yes      |
| 9   | Sales Aggregator (Kafka Streams) | Aggregates all sources into unified topic    | ✅ Yes      |
| 10  | TimescaleDB                      | Time-series database for aggregated results  | ✅ Yes      |
| 11  | Grafana                          | Dashboard & visualization                    | ✅ Yes      |
| 12  | Kafka UI                         | Kafka monitoring and management              | ✅ Yes      |

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              DATA SOURCES                                   │
├─────────────────┬─────────────────────┬─────────────────────────────────────┤
│   PostgreSQL    │     CSV Files       │         SOAP Service                │
│   (São Paulo)   │   (Minas Gerais)    │       (Rio de Janeiro)              │
│                 │                     │                                     │
└────────┬────────┴──────────┬──────────┴──────────────┬──────────────────────┘
         │                   │                         │
         ▼                   ▼                         ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────────────────────┐
│ Kafka Connect   │ │ CSV Connector   │ │       SOAP Connector                │
│ (Debezium CDC)  │ │                 │ │                                     │
└────────┬────────┘ └────────┬────────┘ └──────────────┬──────────────────────┘
         │                   │                         │
         ▼                   ▼                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           APACHE KAFKA                                      │
│  Topics: electromart.public.sales | csv | soap | postgres | sales           │
└─────────────────────────────────────────────────────────────────────────────┘
         │                   │                         │
         └───────────────────┼─────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      SALES AGGREGATOR (Kafka Streams)                       │
│         Consumes: csv, soap, postgres → Produces: unified "sales" topic     │
└─────────────────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          TIMESCALEDB                                        │
│              Time-series database for aggregated sales data                 │
│                    Tables: sales (hypertable)                               │
└─────────────────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            GRAFANA                                          │
│                   Real-time dashboards and analytics                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Kafka Topics

| Topic                      | Source             | Description                    |
| -------------------------- | ------------------ | ------------------------------ |
| `electromart.public.sales` | Debezium CDC       | Raw CDC events from PostgreSQL |
| `csv`                      | CSV Connector      | Sales from CSV files           |
| `soap`                     | SOAP Connector     | Sales from SOAP service        |
| `postgres`                 | Postgres Connector | Enriched PostgreSQL sales      |
| `sales`                    | Sales Aggregator   | Unified sales from all sources |

### Data Regions

Each data source contains data from a different Brazilian state:

| Source       | Region         | Cities                                      |
| ------------ | -------------- | ------------------------------------------- |
| PostgreSQL   | São Paulo      | São Paulo                                   |
| CSV Files    | Minas Gerais   | Belo Horizonte, Contagem, Betim, Uberlândia |
| SOAP Service | Rio de Janeiro | Rio de Janeiro, Niterói, Duque de Caxias    |

## How to Run

```bash
# Start all services
docker-compose up -d

# Check running containers
docker-compose ps

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove all data (clean start)
docker-compose down -v
```

### Access Points

| Service       | URL                         | Credentials                  |
| ------------- | --------------------------- | ---------------------------- |
| Kafka UI      | http://localhost:8888       | -                            |
| Grafana       | http://localhost:3000       | admin / admin                |
| PostgreSQL    | localhost:5432              | electromart / electromart123 |
| TimescaleDB   | localhost:5433              | sales / sales123             |
| SOAP Service  | http://localhost:8080/sales | -                            |
| Kafka Connect | http://localhost:8083       | -                            |

## Results

**Messages on Kafka UI**
<img src="/screenshots/kafka-ui-sales.png" alt="messages"/>

**Results on Grafana**
<img src="/screenshots/graphana-results.png" alt="results"/>

**Observability on Grafana**
<img src="/screenshots/graphana-observability.png" alt="observability"/>
