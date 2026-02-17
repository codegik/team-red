# Kappa Architecture Diagram - Modern Data Pipeline

## High-Level Architecture

```
╔════════════════════════════════════════════════════════════════════════════════════╗
║                              KAPPA ARCHITECTURE                                     ║
║                         Modern Data Pipeline - ElectroRed                          ║
╚════════════════════════════════════════════════════════════════════════════════════╝

┌──────────────────────────────────────────────────────────────────────────────────┐
│                           DATA SOURCES LAYER                                      │
├─────────────────────────┬─────────────────────────┬───────────────────────────────┤
│                         │                         │                               │
│  ┏━━━━━━━━━━━━━━━━━┓   │   ┏━━━━━━━━━━━━━━━┓   │   ┏━━━━━━━━━━━━━━━━━━━━━┓  │
│  ┃   PostgreSQL    ┃   │   ┃   CSV Files   ┃   │   ┃   SOAP Web Service  ┃  │
│  ┃   Sales DB      ┃   │   ┃   /csv-inbox  ┃   │   ┃   WS-* Legacy       ┃  │
│  ┣━━━━━━━━━━━━━━━━━┫   │   ┣━━━━━━━━━━━━━━━┫   │   ┣━━━━━━━━━━━━━━━━━━━━━┫  │
│  ┃ • Real-time     ┃   │   ┃ • Batch files ┃   │   ┃ • Payment confirm   ┃  │
│  ┃ • 50K trans/day ┃   │   ┃ • Every 10sec ┃   │   ┃ • Polled every 5min ┃  │
│  ┃ • CDC-ready     ┃   │   ┃ • 50 rec/file ┃   │   ┃ • XML responses     ┃  │
│  ┗━━━━━━━━━━━━━━━━━┛   │   ┗━━━━━━━━━━━━━━━┛   │   ┗━━━━━━━━━━━━━━━━━━━━━┛  │
│          │               │          │            │            │                  │
└──────────┼───────────────┴──────────┼────────────┴────────────┼──────────────────┘
           │                          │                         │
           │                          │                         │
           └──────────────┬───────────┴─────────────────────────┘
                          │
                          ▼
           ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
           ┃       KAFKA EVENT STREAMING LAYER       ┃
           ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
           ┃  Topics:                                 ┃
           ┃  • sales-events (unified stream)         ┃
           ┃  • top-sales-by-city (aggregated)        ┃
           ┃  • top-salesman-by-country (aggregated)  ┃
           ┃                                           ┃
           ┃  Features:                                ┃
           ┃  • Exactly-once semantics                 ┃
           ┃  • Schema Registry (Avro)                 ┃
           ┃  • Event replay capability                ┃
           ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      STREAM PROCESSING LAYER (Kafka Streams)                     │
├──────────────────────────────────┬──────────────────────────────────────────────┤
│                                  │                                               │
│  ┏━━━━━━━━━━━━━━━━━━━━━━━━━━┓  │   ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓  │
│  ┃   Pipeline 1:            ┃  │   ┃   Pipeline 2:                        ┃  │
│  ┃   Top Sales by City      ┃  │   ┃   Top Salesman by Country            ┃  │
│  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━┫  │   ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫  │
│  ┃                          ┃  │   ┃                                       ┃  │
│  ┃ 1. Read: sales-events    ┃  │   ┃ 1. Read: sales-events                ┃  │
│  ┃ 2. Group by: city        ┃  │   ┃ 2. Group by: seller + country        ┃  │
│  ┃ 3. Window: 1 day         ┃  │   ┃ 3. Window: 30 days (monthly)         ┃  │
│  ┃ 4. Aggregate: SUM(sales) ┃  │   ┃ 4. Aggregate: SUM(sales)             ┃  │
│  ┃ 5. Output: Kafka topic   ┃  │   ┃ 5. Output: Kafka topic               ┃  │
│  ┃                          ┃  │   ┃                                       ┃  │
│  ┃ Window Type: Tumbling    ┃  │   ┃ Window Type: Tumbling                ┃  │
│  ┃ Late Events: Handled     ┃  │   ┃ Ranking: Top performers              ┃  │
│  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┛  │   ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛  │
│           │                      │                │                              │
└───────────┼──────────────────────┴────────────────┼──────────────────────────────┘
            │                                       │
            └───────────────┬───────────────────────┘
                            │
                            ▼
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃       SERVING LAYER                 ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
            ┃                                      ┃
            ┃  ┌──────────────────────────────┐   ┃
            ┃  │  PostgreSQL Results DB       │   ┃
            ┃  │  • top_sales_by_city         │   ┃
            ┃  │  • top_salesman_by_country   │   ┃
            ┃  └──────────────────────────────┘   ┃
            ┃              │                       ┃
            ┃              ▼                       ┃
            ┃  ┌──────────────────────────────┐   ┃
            ┃  │  REST API (Spring Boot)      │   ┃
            ┃  │  • GET /api/v1/top-sales/    │   ┃
            ┃  │         by-city              │   ┃
            ┃  │  • GET /api/v1/top-salesmen/ │   ┃
            ┃  │         by-country           │   ┃
            ┃  │  • Swagger UI enabled        │   ┃
            ┃  └──────────────────────────────┘   ┃
            ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         OBSERVABILITY & LINEAGE LAYER                            │
├──────────────────┬──────────────────────┬───────────────────────────────────────┤
│                  │                      │                                        │
│  ┏━━━━━━━━━━━┓  │  ┏━━━━━━━━━━━━━━┓  │  ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓  │
│  ┃ Prometheus┃  │  ┃   Grafana    ┃  │  ┃   OpenLineage (Planned)      ┃  │
│  ┣━━━━━━━━━━━┫  │  ┣━━━━━━━━━━━━━━┫  │  ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫  │
│  ┃ • Metrics ┃  │  ┃ • Dashboards ┃  │  ┃ • Data lineage tracking      ┃  │
│  ┃ • Alerts  ┃  │  ┃ • Charts     ┃  │  ┃ • Upstream/downstream view   ┃  │
│  ┃ • JMX     ┃  │  ┃ • Real-time  ┃  │  ┃ • Transformation tracking    ┃  │
│  ┗━━━━━━━━━━━┛  │  ┗━━━━━━━━━━━━━━┛  │  ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛  │
└─────────────────┴──────────────────────┴───────────────────────────────────────┘


## Data Flow Details

### Flow 1: PostgreSQL → Kafka → Aggregation
```
PostgreSQL Sales Table
    │ (INSERT every 5 seconds)
    ▼
Kafka Producer (SaleEventProducer)
    │ (Publish to sales-events)
    ▼
Kafka Topic: sales-events
    │
    ▼
Kafka Streams: TopSalesByCityTopology
    │ (Group by city, window 1-day, aggregate)
    ▼
Kafka Topic: top-sales-by-city
    │
    ▼
Kafka Consumer: TopSalesByCityConsumer
    │
    ▼
PostgreSQL Results DB: top_sales_by_city table
    │
    ▼
REST API: GET /api/v1/top-sales/by-city
```

### Flow 2: CSV Files → Kafka → Aggregation
```
CSV Generator
    │ (Generate file every 10 seconds)
    ▼
/data/csv-inbox/sales_*.csv
    │ (File watcher or Kafka Connect)
    ▼
Kafka Topic: sales-events
    │
    ▼
[Same processing as Flow 1]
```

### Flow 3: SOAP Service → Kafka → Enrichment
```
SOAP Web Service
    │ (Poll every 5 minutes)
    ▼
Payment Confirmations XML
    │
    ▼
Kafka Topic: payment-confirmations
    │ (Join with sales-events)
    ▼
Enriched Sales Stream
```

## Key Characteristics

### Kappa Architecture Benefits
✅ **Single Processing Path**: No batch/speed layer complexity
✅ **Event Replay**: Reprocess historical data by replaying Kafka topics
✅ **Exactly-Once**: Kafka Streams guarantees no duplicates
✅ **Real-Time**: Sub-second latency from source to API
✅ **Scalable**: Horizontally scale stream processors
✅ **Fault-Tolerant**: Kafka handles node failures automatically

### Windowing Strategy
- **City Aggregations**: 1-day tumbling windows (00:00 to 23:59)
- **Salesman Aggregations**: 30-day tumbling windows (monthly)
- **Late Events**: Grace period for out-of-order events
- **Watermarks**: Event-time based processing

## Technology Stack Summary

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Ingestion | Kafka Connect, Custom Producers | Unified event streaming |
| Processing | Kafka Streams (Java) | Real-time aggregations |
| Storage | PostgreSQL | Source data & results |
| API | Spring Boot 3.2 | REST endpoints |
| Monitoring | Prometheus + Grafana | Metrics & dashboards |
| Lineage | OpenLineage | Data flow tracking |
| Orchestration | Docker Compose | Local deployment |

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Compose                       │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  │
│  │ Zookeeper   │  │    Kafka    │  │Schema Registry│  │
│  │   :2181     │  │   :9092     │  │    :8081     │  │
│  └─────────────┘  └─────────────┘  └──────────────┘  │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  │
│  │ PostgreSQL  │  │ PostgreSQL  │  │  Prometheus  │  │
│  │  Source DB  │  │ Results DB  │  │    :9090     │  │
│  │   :5432     │  │   :5433     │  └──────────────┘  │
│  └─────────────┘  └─────────────┘                    │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐  │
│  │Data Sources │  │   Stream    │  │  Results API │  │
│  │   :8080     │  │ Processors  │  │    :8082     │  │
│  │             │  │   :8081     │  │              │  │
│  └─────────────┘  └─────────────┘  └──────────────┘  │
│                                                         │
│  ┌─────────────┐                                       │
│  │   Grafana   │                                       │
│  │    :3000    │                                       │
│  └─────────────┘                                       │
└─────────────────────────────────────────────────────────┘
```

---
**Created**: February 17, 2026
**Architecture**: Kappa Pattern
**Status**: ✅ Ready for Demo

