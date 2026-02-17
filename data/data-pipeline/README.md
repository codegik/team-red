# Modern Data Pipeline

## ✅ Status: IMPLEMENTATION COMPLETE!

See [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) for full details.

**Quick Start:**
```bash
./start.sh
```

**API Documentation:** http://localhost:8082/swagger-ui.html

---

- 23.MAR.2026 - Data Kata
Must Create a Modern Data Pipeline with:
	1. Ingestion for 3 different data sources (Relational DB, File system and Traditional WS-*) ✅
	2. Modern Processing with Spark, Flink or Kafka Streams ✅ (Kafka Streams)
	3. Data Lineage ✅ (OpenLineage ready)
	4. Observability ✅ (Prometheus + Grafana)
	5. Pipelines must have at least 2 pipelines: ✅
		a. Top Sales per City ✅
		b. Top Salesman in the whole country ✅
	6. The final Aggregated results mut be in a dedicated DB and API ✅
	7. Restrictions: ✅
		a. Python ✅ (All Java)
		b. Red-Shift ✅ (PostgreSQL)
		c. Hadoop ✅ (Kafka Streams)

## Architecture

**Pattern:** Kappa Architecture (streaming-first)

See [ARCHITECTURE.md](./ARCHITECTURE.md) for detailed diagrams.

## Timeline

- [x] 19/12/25 - First round of study contents
- [x] 23/01/26 - Studied components about the solution, had internal team discussion about it, and expose the lead to receive feedback
- [x] 06/02/26 - Created arch diagrams
- [x] 17/02/26 - **IMPLEMENTATION COMPLETE**
- [ ] 23/02/26 - Finilize arch diagrams, ask feedback (6 days remaining)
- [ ] 09/03/26 - Dry runs

## Documentation

- **[IMPLEMENTATION_README.md](./IMPLEMENTATION_README.md)** - Complete user guide
- **[IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md)** - Technical summary
- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - Architecture diagrams
- **[start.sh](./start.sh)** - Quick start script


