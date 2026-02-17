# Kappa Architecture Data Pipeline

Modern Data Pipeline implementation using Kappa Architecture pattern for real-time data processing.

## Architecture Overview

This project implements a **Kappa Architecture** for processing sales data from multiple sources:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            DATA SOURCES                                      │
├──────────────────┬──────────────────────┬─────────────────────────────────  │
│  PostgreSQL      │   CSV Files          │   SOAP Web Service               │
│  (Real-time DB)  │   (Batch imports)    │   (Legacy system)                │
└────────┬─────────┴──────────┬───────────┴─────────┬────────────────────────┘
         │                    │                      │
         └────────────────────┴──────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │   KAFKA BROKER   │
                    │   (Event Stream) │
                    └─────────┬────────┘
                              │
                              ▼
                   ┌────────────────────────┐
                   │  KAFKA STREAMS         │
                   │  Stream Processors     │
                   ├────────────────────────┤
                   │ • Top Sales by City    │
                   │ • Top Salesman/Country │
                   └───────────┬────────────┘
                               │
                 ┌─────────────┴──────────────┐
                 ▼                            ▼
        ┌─────────────────┐         ┌─────────────────┐
        │ Results Database│         │   REST API      │
        │   (PostgreSQL)  │◄────────│  (Query Layer)  │
        └─────────────────┘         └─────────────────┘
                                              │
                                              ▼
                                    ┌──────────────────┐
                                    │  Swagger UI      │
                                    │  Grafana         │
                                    │  Prometheus      │
                                    └──────────────────┘
```

## Project Structure

```
data-pipeline/
├── pom.xml                      # Parent POM
├── docker-compose.yml           # All services orchestration
├── common-models/               # Shared Avro schemas and models
├── data-source-generators/      # Mock data generators (PostgreSQL, CSV, SOAP)
├── stream-processors/           # Kafka Streams applications
├── results-api/                 # REST API for querying results
├── observability/               # Prometheus & Grafana configs
└── README.md                    # This file
```

## Components

### 1. Data Sources
- **PostgreSQL Generator**: Continuously generates sales transactions
- **CSV Generator**: Creates CSV files every 10 seconds
- **SOAP Service**: Mock WS-* web service for payment confirmations

### 2. Stream Processing (Kafka Streams)
- **Top Sales by City**: Aggregates sales by city using 1-day tumbling windows
- **Top Salesman by Country**: Aggregates performance by salesman and country (30-day windows)

### 3. Serving Layer
- **PostgreSQL Results DB**: Stores aggregated results
- **REST API**: Exposes endpoints for querying top sales and salesmen
- **Swagger UI**: Interactive API documentation at http://localhost:8082/swagger-ui.html

### 4. Observability
- **Prometheus**: Metrics collection from all services
- **Grafana**: Dashboards for monitoring pipeline health
- **Actuator**: Health checks and metrics endpoints

## Requirements

- Docker & Docker Compose
- Java 17+ (for local development)
- Maven 3.9+ (for local builds)
- 8GB RAM minimum (for all services)

## Quick Start

### 1. Start All Services

```bash
# Start the entire pipeline
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f
```

### 2. Wait for Services to be Healthy

```bash
# Check if all services are up (wait 2-3 minutes)
docker-compose ps
```

### 3. Verify Data Flow

**Check data generators:**
```bash
# PostgreSQL generator
curl http://localhost:8080/actuator/health

# View generated CSV files
docker exec kappa-data-sources ls -la /data/csv-inbox

# SOAP service WSDL
curl http://localhost:8080/ws/paymentValidation.wsdl
```

**Check Kafka topics:**
```bash
docker exec kappa-kafka kafka-topics --bootstrap-server localhost:9093 --list
```

### 4. Query Results via REST API

**Top Sales by City:**
```bash
# Get top 10 cities by sales
curl http://localhost:8082/api/v1/top-sales/by-city?limit=10

# Filter by country
curl http://localhost:8082/api/v1/top-sales/by-city?country=Brazil&limit=5
```

**Top Salesmen by Country:**
```bash
# Get top 10 salesmen
curl http://localhost:8082/api/v1/top-salesmen/by-country?limit=10

# Filter by specific country
curl http://localhost:8082/api/v1/top-salesmen/by-country?country=Brazil&limit=5
```

### 5. Access Monitoring Dashboards

- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Kafka Connect**: http://localhost:8083

## Demo: Visualizing the Two Pipelines

### Pipeline 1: Top Sales per City

```bash
# Watch real-time aggregations
watch -n 2 'curl -s http://localhost:8082/api/v1/top-sales/by-city?limit=5 | jq'
```

Expected output:
```json
[
  {
    "id": 1,
    "city": "São Paulo",
    "country": "Brazil",
    "totalSales": 234567.89,
    "transactionCount": 45,
    "windowStart": 1708128000000,
    "windowEnd": 1708214400000,
    "updatedAt": "2026-02-17T10:30:00"
  },
  ...
]
```

### Pipeline 2: Top Salesman in the Whole Country

```bash
# Watch top salesmen
watch -n 2 'curl -s http://localhost:8082/api/v1/top-salesmen/by-country?limit=5 | jq'
```

Expected output:
```json
[
  {
    "id": 1,
    "sellerCode": "SEL003",
    "sellerName": "Pedro Costa",
    "country": "Brazil",
    "totalSales": 456789.12,
    "transactionCount": 78,
    "windowStart": 1705536000000,
    "windowEnd": 1708214400000,
    "updatedAt": "2026-02-17T10:30:00"
  },
  ...
]
```

## Development

### Build Locally

```bash
# Build all modules
mvn clean package

# Build specific module
cd data-source-generators
mvn clean package
```

### Run Individual Services Locally

```bash
# Start dependencies first
docker-compose up -d postgres-source postgres-results kafka zookeeper

# Run data generators
cd data-source-generators
mvn spring-boot:run

# Run stream processors
cd stream-processors
mvn spring-boot:run

# Run API
cd results-api
mvn spring-boot:run
```

## Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Stream Processing | Kafka Streams | Real-time aggregation |
| Message Broker | Apache Kafka | Event streaming backbone |
| Source Database | PostgreSQL | Sales transactions |
| Results Database | PostgreSQL | Aggregated results |
| API Framework | Spring Boot | REST API |
| Observability | Prometheus + Grafana | Monitoring |
| Data Lineage | OpenLineage (planned) | Track data flow |
| Build Tool | Maven | Java build automation |
| Orchestration | Docker Compose | Service deployment |

## Key Design Decisions

### Why Kappa over Lambda?
1. **Unified Processing**: Single stream processing path (no batch/speed layer complexity)
2. **Real-time First**: All sources can be streamed (PostgreSQL via CDC, CSV via file watching)
3. **Simpler Architecture**: Easier to maintain and debug
4. **Data Volume**: ~60K transactions/day manageable with streaming

### Why Kafka Streams over Flink?
1. **Embedded Library**: No separate cluster management
2. **Exactly-Once Semantics**: Built-in transactional support
3. **Simpler Deployment**: Runs as part of Java application
4. **Faster Development**: Less operational overhead

## Restrictions Compliance

✅ **No Python**: All services in Java  
✅ **No Redshift**: Using PostgreSQL  
✅ **No Hadoop**: Using Kafka Streams  

## Troubleshooting

### Services won't start
```bash
# Check Docker resources
docker system df
docker system prune -a

# Increase Docker memory to 8GB in Docker Desktop settings
```

### No data in results API
```bash
# Check if stream processors are running
docker logs kappa-stream-processors

# Verify Kafka has data
docker exec kappa-kafka kafka-console-consumer \
  --bootstrap-server localhost:9093 \
  --topic sales-events \
  --from-beginning \
  --max-messages 10
```

### Database connection errors
```bash
# Check PostgreSQL is healthy
docker exec kappa-postgres-source pg_isready -U electrored
docker exec kappa-postgres-results pg_isready -U electrored
```

## Stop and Cleanup

```bash
# Stop all services
docker-compose down

# Remove volumes (WARNING: deletes all data)
docker-compose down -v

# Remove images
docker-compose down --rmi all
```

## Timeline

- [x] 06/02/26 - Architecture design complete
- [x] 17/02/26 - Implementation in progress
- [ ] 23/02/26 - Finalize architecture diagrams
- [ ] 09/03/26 - Dry runs and demos

## License

Internal team project - ElectroRed Data Pipeline Team

---

**Built with ❤️ using Kappa Architecture**

