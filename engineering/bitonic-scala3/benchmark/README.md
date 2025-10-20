# Bitonic Benchmark Suite

This directory contains benchmarking tools for testing three endpoints:

- `POST /bitonic?n=...&l=...&r=...` — Direct calculation (no cache)
- `POST /bitonic-redis?n=...&l=...&r=...` — Redis cache
- `POST /bitonic-memcached?n=...&l=...&r=...` — Memcached cache

## Requirements

- Docker and Docker Compose
- (Optional) Python 3.9+ for standalone scripts

## Running Benchmarks

### Quick Start

1. **Start all services (app, cache, monitoring, and benchmark):**
   ```bash
   docker compose up
   ```

2. **View results in Grafana:**
   - URL: http://localhost:3000
   - Username: `admin`
   - Password: `admin`
   - Dashboard: "Bitonic Service Performance Comparison"

### What Gets Measured

The Grafana dashboard shows:

**Performance Metrics:**
- Response Time Comparison (Standard vs Redis vs Memcached)
- Throughput (Requests/Second)
- 95th Percentile Response Time (P95)
- 99th Percentile Response Time (P99)
- Error Rate Comparison

**Cache Metrics:**
- Redis Cache Hit/Miss Rate
- Memcached Cache Hit/Miss Rate
- Cache performance over time

**Visual Indicators:**
- Color-coded thresholds for response times:
  - 🟢 Green (0-50ms): Excellent
  - 🟡 Yellow (50-100ms): Good
  - 🟠 Orange (100-150ms): Attention needed
  - 🔴 Red (>150ms): Problematic

## Benchmark Payloads

The benchmark uses `payloads.json` containing 50,000 test cases with:
- **Array sizes**: 0-1,000 (random distribution)
- **Range types**: Random l and r values
- **Purpose**: Comprehensive stress testing of all three endpoints

## Understanding Percentiles (P95, P99)

**What are percentiles?**
- **P95 (95th Percentile)**: 95% of requests were faster than this value (only 5% were slower)
- **P99 (99th Percentile)**: 99% of requests were faster than this value (only 1% were slower)

**Why they matter:**
- The **mean** (average) can hide performance problems
- Percentiles show the **worst-case experience** for your users
- High P99 values indicate some users are experiencing poor performance

**Example:**
```
If Redis P99 = 50ms, it means:
- 99% of requests completed in ≤ 50ms (excellent!)
- Only 1% took longer (outliers)
```

## Architecture

The benchmark infrastructure consists of:

```
┌─────────────┐
│   K6 Load   │ ──── Sends HTTP requests ───▶ ┌──────────────┐
│  Generator  │                                 │  Bitonic App │
└─────────────┘                                 │  (3 endpoints)│
       │                                        └──────┬───────┘
       │                                               │
       │ Sends metrics                          Uses cache
       ▼                                               ▼
┌─────────────┐                              ┌─────────────────┐
│  InfluxDB   │ ◀──── Scraped by ────────   │ Redis/Memcached │
│ (Time-series│                        │     └─────────────────┘
│   Database) │                        │              │
└──────┬──────┘                   ┌────┴────┐        │
       │                          │Telegraf │◀───────┘
       │ Queries                  │(Scraper)│  Exports metrics via:
       ▼                          └─────────┘  - Redis Exporter (port 9121)
┌─────────────┐                                - Memcached Exporter (port 9150)
│   Grafana   │ ──── Visualizes ────▶ Dashboard
│ (Dashboard) │
└─────────────┘
```

### Cache Metrics Collection

**Redis Metrics:**
- Collected via [Redis Exporter](https://github.com/oliver006/redis_exporter) (port 9121)
- Key metrics: `redis_keyspace_hits_total`, `redis_keyspace_misses_total`

**Memcached Metrics:**
- Collected via [Memcached Exporter](https://github.com/prometheus/memcached_exporter) (port 9150)
- Key metrics: `memcached_commands_total{command="get",status="hit/miss"}`

**Collection Flow:**
1. Exporters scrape cache statistics from Redis/Memcached
2. Telegraf collects Prometheus-formatted metrics from exporters (every 10s)
3. Telegraf sends metrics to InfluxDB (database: `k6`, measurement: `prometheus`)
4. Grafana queries InfluxDB and visualizes cache hit/miss rates

## Resource Limits

All containers have CPU and memory limits configured:
- **App**: 1 CPU, 1GB RAM
- **Redis**: 0.25 CPU, 256MB RAM
- **Memcached**: 0.25 CPU, 128MB RAM
- **InfluxDB**: 0.5 CPU, 512MB RAM
- **Grafana**: 0.5 CPU, 512MB RAM
- **K6**: 0.5 CPU, 256MB RAM

This ensures consistent and fair performance comparison.

## Troubleshooting

### Dashboard shows no data
- Wait for K6 to start sending requests (starts automatically with `docker compose up`)
- Check if InfluxDB is running: `docker ps | grep influx`
- Verify K6 is running: `docker logs bitonic-scala3-k6-1`

### Cache metrics not showing
1. **Verify exporters are running:**
   ```bash
   docker ps | grep exporter
   ```

2. **Test exporters directly:**
   ```bash
   # Redis Exporter
   curl http://localhost:9121/metrics | grep keyspace

   # Memcached Exporter
   curl http://localhost:9150/metrics | grep commands
   ```

3. **Check Telegraf logs:**
   ```bash
   docker logs telegraf
   ```

4. **Query InfluxDB directly:**
   ```bash
   docker exec bitonic-scala3-influxdb-1 influx -database 'k6' -execute 'SELECT * FROM prometheus WHERE cache_type = '\''redis'\'' LIMIT 5'
   ```

### K6 finished but want to run again
```bash
docker compose restart k6
```

### Clear all metrics and start fresh
```bash
docker compose down -v  # Removes volumes (InfluxDB data)
docker compose up
```

