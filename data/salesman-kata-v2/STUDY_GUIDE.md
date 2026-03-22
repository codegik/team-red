# TOP Salesman — Data KATA: Guia de Estudos para Apresentação

> Foco em código, siglas, stack e fluxo prático.

---

## Stack de Tecnologias (versões exatas)

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem backend | Java | 25 |
| Linguagem data sources | Node.js | 25 |
| Build Java | Maven | 3.9+ |
| Streaming | Apache Kafka | 3.7.1 |
| Streaming API | Kafka Streams | 3.7.0 |
| CDC | Debezium | 2.5 |
| Schema Registry | Confluent Schema Registry | 7.6.0 |
| Source DB | PostgreSQL | 18 |
| Sink DB | TimescaleDB | latest-pg15 |
| Object Storage | MinIO | latest |
| Testes | JUnit 5 (Jupiter) | 5.10.2 |
| JSON | Jackson | 2.16.1 |
| JSON Schema Validator | networknt | 1.3.3 |
| Observabilidade | OpenTelemetry | 1.50.0 |
| Métricas | Prometheus | 2.49.1 |
| Dashboards | Grafana | 10.3.1 |
| Tracing | Jaeger | 1.54 |
| OTel Collector | OpenTelemetry Collector | 0.96.0 |
| UI Kafka | Kafka UI | 0.7.2 |

---

## Siglas e Conceitos Chave

| Sigla/Termo | Significado | No projeto |
|---|---|---|
| **CDC** | Change Data Capture | Debezium captura INSERTs do PostgreSQL fonte sem alterar o app |
| **WAL** | Write-Ahead Log | Log binário do PostgreSQL; Debezium lê daqui via `pgoutput` plugin |
| **Kafka Streams** | API de stream processing embutida no Kafka | Usado no `SalesEnricher` e `SalesAggregator` |
| **GlobalKTable** | Tabela replicada em todas as partições de um tópico Kafka | Usada para joins de produtos, vendedores e lojas no enricher |
| **KStream** | Fluxo infinito de eventos imutáveis | Representação dos eventos de venda no `SalesEnricher` |
| **DLQ** | Dead Letter Queue | Tópico `sales-dlq` recebe registros inválidos que não passam validação |
| **Schema Registry** | Serviço que armazena e versiona schemas Avro/JSON | Confluent Schema Registry na porta 8081 |
| **Avro** | Formato de serialização binário com schema | Usado nos tópicos Kafka com Schema Registry |
| **TimescaleDB** | Extensão PostgreSQL para time-series | Sink final dos dados agregados de vendas |
| **MinIO** | Object storage compatível com S3 | Recebe CSVs dos data sources |
| **SOAP** | Simple Object Access Protocol | Protocolo legado XML usado pela fonte de dados mock |
| **OTel / OTEL** | OpenTelemetry | Framework de observabilidade (traces, metrics, logs) |
| **pgoutput** | Plugin de decodificação lógica do PostgreSQL | Converte WAL em eventos que o Debezium lê |
| **Debezium** | Framework de CDC open-source | Registrado como conector no Kafka Connect |
| **ExtractNewRecordState** | SMT do Debezium | Single Message Transform que extrai apenas o estado novo do envelope CDC |
| **SMT** | Single Message Transform | Transformações inline no Kafka Connect antes de publicar no tópico |

---

## Fluxo de Dados (passo a passo)

```
[Node.js] postgresql/index.js      → INSERT no PostgreSQL (a cada 5s)
                                         ↓
                                   Debezium (CDC via WAL + pgoutput)
                                         ↓
                              Kafka Connect registrado por:
                              PostgresConnector.java (one-shot)
                                         ↓
                    Tópicos: electromart.public.sales
                             electromart.public.products
                             electromart.public.salesmen
                             electromart.public.stores
                                         ↓
                              SalesEnricher.java (Kafka Streams)
                              [join KStream × GlobalKTable]
                              [normaliza sale_id → PG-{id}]
                              [valida schema raw-postgres-value]
                                         ↓
                                   raw_postgres topic

[Node.js] csv-files/index.js       → CSV upload para MinIO (a cada 5s)
                                         ↓
                              MinIO webhook → CsvConnector.java
                              [HTTP webhook porta 8085]
                              [lê CSV do MinIO, parseia, valida]
                                         ↓
                                   raw_csv topic

[Node.js] soap/index.js            → Mock SOAP server (porta 8080)
                                         ↓
                              SoapConnector.java (poll a cada 5s)
                              [parseia XML SOAP]
                              [valida schema raw-soap-value]
                                         ↓
                                   raw_soap topic

                    raw_postgres + raw_csv + raw_soap
                                         ↓
                          SalesAggregator.java (Kafka Streams)
                          [merge 3 streams]
                          [adiciona: source, trace_id, ingested_at]
                          [valida campos obrigatórios]
                                         ↓
                          ┌──────────────┴──────────────┐
                       sales topic                 sales-dlq topic
                          ↓
                   SalesConsumer.java
                   [deduplica por sale_id]
                   [insere no TimescaleDB]
                   [expõe REST API porta 8090]
```

---

## Serviços Java — O que cada classe faz

### `postgres-connector-source`

| Classe | Responsabilidade |
|---|---|
| `PostgresConnector.java` | **One-shot service**: registra o conector Debezium via REST API do Kafka Connect. Configura: plugin `pgoutput`, publication, replication slot, tabelas a capturar (sales, products, salesmen, stores), e aplica o SMT `ExtractNewRecordState`. Morre após registrar. |

### `postgres-enricher`

| Classe | Responsabilidade |
|---|---|
| `SalesEnricher.java` | **Kafka Streams topology**: consome `electromart.public.sales` como KStream. Carrega products, salesmen, stores como GlobalKTables. Faz join para enriquecer cada venda. Normaliza `sale_id` para formato `PG-{id}`. Valida contra schema `raw-postgres-value`. Publica em `raw_postgres`. |
| `SchemaValidator.java` | Busca schema JSON no Confluent Schema Registry e valida um `JsonNode` contra ele. |

### `csv-connector-source`

| Classe | Responsabilidade |
|---|---|
| `CsvConnector.java` | **HTTP webhook** (porta 8085): recebe notificações do MinIO, lê o arquivo CSV, parseia linha a linha, valida cada registro contra schema `raw-csv-value`, publica em `raw_csv`. Move arquivos processados para bucket separado. Erros vão para DLQ. |
| `SchemaValidator.java` | Idêntico ao do postgres-enricher, valida contra `raw-csv-value`. |

### `soap-connector-source`

| Classe | Responsabilidade |
|---|---|
| `SoapConnector.java` | **Polling a cada 5s** contra o mock SOAP (porta 8080). Parseia XML response (`sale:record` elements). Mapeia campos XML → JSON. Valida contra schema `raw-soap-value`. Publica em `raw_soap`. Health endpoint na porta 8087. |
| `SchemaValidator.java` | Valida contra `raw-soap-value`. |

### `sales-aggregator`

| Classe | Responsabilidade |
|---|---|
| `SalesAggregator.java` | **Kafka Streams topology central**: faz merge de `raw_postgres`, `raw_csv`, `raw_soap` num único stream. Adiciona metadados: `source`, `trace_id` (UUID), `ingested_at` (timestamp). Valida campos obrigatórios: `sale_id`, `source`, `sale_timestamp`, `total_amount`. Roteia válidos → `sales`, inválidos → `sales-dlq`. Instrumenta métricas com OpenTelemetry. |

### `sales-consumer`

| Classe | Responsabilidade |
|---|---|
| `SalesConsumer.java` | **Entry point**: consome tópico `sales`. Delega escrita para `SalesWriter`. Inicia `HttpApiServer` (8090) e health server (8086). Deduplica por `sale_id + sale_timestamp`. Rastreia métricas (latência, duplicatas, fallbacks, receita). |
| `SalesWriter.java` | Insere registros no TimescaleDB. Usa `ON CONFLICT DO NOTHING` para deduplicação. Faz fallback para `Instant.now()` em timestamps inválidos. Retorna `SalesWriteResult`. |
| `HttpApiServer.java` | REST API com 4 endpoints. Suporta filtros de data ISO-8601 via query params `from` e `to`. |
| `AggregatesRepository.java` | Queries no TimescaleDB: top cidades por receita, top vendedores por país, summary. Suporta modo range (com datas) ou latest bucket. |
| `AggregatesService.java` | Interface funcional para as queries de agregação. |
| `Database.java` | Utilitários JDBC: `waitForDatabase`, `connect`, `isValid`, `closeQuietly`. |
| `DatabaseConfig.java` | Record com host/port/user/password/database → gera JDBC URL. |
| `Env.java` | Lê variáveis de ambiente com defaults. |
| `ConnectionProvider.java` | `@FunctionalInterface` para injeção de conexão (facilita testes). |
| `SalesWriteResult.java` | Record de resultado: `inserted`, `timestampFallbackUsed`, `saleId`, `source`, `totalAmount`, `pickedUpAt`. |
| `AggregateResult.java` | Record de resultado: `mode` ("range"/"latest"), `rows` (List<Map>). |
| `ApiException.java` | `RuntimeException` com `statusCode` HTTP. |

---

## Data Sources Node.js — O que cada módulo faz

| Arquivo | Responsabilidade |
|---|---|
| `postgresql/src/index.js` | Conecta no PostgreSQL (`electromart` DB). Gera vendas aleatórias (produtos, vendedores, lojas). Insere 10 vendas iniciais, depois 1-5 por intervalo de 5s. |
| `csv-files/src/index.js` | Gera arquivos CSV com 1-5 vendas. Nome: `sales_YYYYMMDD_HHMMSS.csv`. Faz upload para MinIO no bucket `sales-csv` a cada 5s. |
| `soap/src/index.js` | Mock SOAP server (Express.js) na porta 8080. Endpoint `/sales`: retorna 1-5 vendas em XML SOAP. Endpoint `/health`. |

---

## Schemas JSON (Schema Registry)

| Schema | Tópico | Campos obrigatórios | Padrões notáveis |
|---|---|---|---|
| `raw-postgres-value.json` | `raw_postgres` | sale_id, source, sale_timestamp, total_amount | `sale_id` pattern: `^PG-[0-9]+$` · `source` const: `"postgres"` |
| `raw-csv-value.json` | `raw_csv` | sale_id, sale_timestamp, total_amount | Todos strings (exceto quantity=int) |
| `raw-soap-value.json` | `raw_soap` | sale_id, sale_timestamp, total_amount | Mesma estrutura do CSV |
| `sales-value.json` | `sales` | sale_id, source, sale_timestamp, total_amount | `source` enum: `csv\|soap\|postgres` · `additionalProperties: false` |

---

## REST API Endpoints

Base URL: `http://localhost:8090`

| Endpoint | Descrição | Params opcionais |
|---|---|---|
| `GET /health` | Health check | — |
| `GET /api/aggregates/top-sales-per-city` | Top cidades por receita | `from`, `to`, `limit`, `cityLimit` |
| `GET /api/aggregates/top-salesman-country` | Top vendedores por país | `from`, `to`, `limit`, `salesmanLimit` |
| `GET /api/aggregates/summary` | Resumo geral de vendas | `from`, `to` |

Exemplo com filtro de data:
```bash
curl "http://localhost:8090/api/aggregates/summary?from=2026-03-13T00:00:00Z&to=2026-03-13T23:59:59Z"
```

---

## Portas dos Serviços

| Serviço | Porta | Credenciais |
|---|---|---|
| REST API (Sales Consumer) | 8090 | — |
| Health (Sales Consumer) | 8086 | — |
| SOAP Mock | 8080 | — |
| CSV Connector webhook | 8085 | — |
| SOAP Connector health | 8087 | — |
| Kafka Connect (Debezium) | 8083 | — |
| Kafka UI | 8888 | — |
| Schema Registry | 8081 | — |
| Grafana | 3000 | admin/admin |
| Prometheus | 9090 | — |
| Jaeger UI | 16686 | — |
| MinIO Console | 9001 | minioadmin/minioadmin123 |
| PostgreSQL (fonte) | 5432 | electromart/electromart123 |
| TimescaleDB (sink) | 5433 | sales/sales123 |

---

## Testes — Cobertura por Serviço

| Arquivo de Teste | O que testa |
|---|---|
| `SalesAggregatorTest.java` | `addSourceMetadata`, validação de campos obrigatórios, roteamento DLQ |
| `SalesEnricherTest.java` | Join de streams, normalização de IDs, enriquecimento de campos |
| `CsvConnectorTest.java` | Parse de CSV linha a linha, validação de schema |
| `SoapConnectorTest.java` | Parse XML SOAP → JSON, mapeamento de campos |
| `PostgresConnectorTest.java` | Construção da config do conector Debezium |
| `SalesWriterTest.java` | Binding de PreparedStatement, fallback de timestamp, `ON CONFLICT` |
| `HttpApiServerTest.java` | Endpoints REST, parse de query params, erros HTTP |

Executar testes:
```bash
./run_tests.sh              # auto-detecta Java 25 local ou usa Docker
./run_tests.sh docker       # força Docker (maven:3.9-eclipse-temurin-25)
cd services/sales-consumer && mvn test -Dtest=SalesWriterTest
```

---

## Roteiro de Apresentação Prática

### 1. Explicar a arquitetura (2 min)
- Mostrar o diagrama de fluxo acima
- Destacar as 3 fontes heterogêneas: CDC (push), CSV (webhook), SOAP (pull)
- Mencionar normalização via Schema Registry

### 2. Subir o ambiente (3 min)
```bash
docker compose up -d --build
docker compose ps              # verificar saúde dos containers
docker compose logs -f         # mostrar dados fluindo
```

### 3. Mostrar dados chegando nas fontes (2 min)
```bash
docker compose logs -f postgresql-source    # inserts no PostgreSQL
docker compose logs -f csv-files-source     # uploads MinIO
docker compose logs -f soap-source          # respostas SOAP
```

### 4. Mostrar Kafka UI (2 min)
- `http://localhost:8888`
- Tópicos: `electromart.public.sales`, `raw_postgres`, `raw_csv`, `raw_soap`, `sales`, `sales-dlq`
- Mostrar mensagens fluindo em tempo real

### 5. Mostrar Schema Registry (1 min)
```bash
curl http://localhost:8081/subjects          # listar schemas registrados
curl http://localhost:8081/subjects/sales-value/versions/latest
```

### 6. Consultar a API REST (2 min)
```bash
curl http://localhost:8090/health
curl http://localhost:8090/api/aggregates/top-sales-per-city
curl http://localhost:8090/api/aggregates/top-salesman-country
curl http://localhost:8090/api/aggregates/summary
```

### 7. Mostrar observabilidade (2 min)
- Grafana: `http://localhost:3000` — dashboards de receita, latência, duplicatas
- Jaeger: `http://localhost:16686` — traces com `sale.id` e `sale.source`

### 8. Executar testes (1 min)
```bash
./run_tests.sh docker
```

### 9. Mostrar código-chave (3 min)

**SalesEnricher** — join de streams:
```
services/postgres-enricher/src/main/java/com/electromart/SalesEnricher.java
```

**SalesAggregator** — merge + validação + DLQ:
```
services/sales-aggregator/src/main/java/com/electromart/SalesAggregator.java
```

**SalesConsumer** — deduplicação + insert + API:
```
services/sales-consumer/src/main/java/com/electromart/SalesConsumer.java
services/sales-consumer/src/main/java/com/electromart/SalesWriter.java
services/sales-consumer/src/main/java/com/electromart/HttpApiServer.java
```

---

## Pontos de Atenção para a Apresentação

- **CDC vs Polling**: explicar por que PostgreSQL usa CDC (menos intrusivo, captura mudanças no WAL) enquanto SOAP usa polling (protocolo legado sem suporte a eventos)
- **GlobalKTable vs KStream**: GlobalKTable é carregada completamente em memória local de cada instância; ideal para dados de referência (produtos, lojas)
- **DLQ**: mostrar que registros inválidos não bloqueiam o pipeline — vão para `sales-dlq` para análise posterior
- **Deduplicação**: `ON CONFLICT DO NOTHING` no TimescaleDB garante idempotência mesmo com reprocessamento
- **Schema Registry**: garante contrato entre produtores e consumidores; rejeita mensagens fora do schema
- **OpenTelemetry**: instrumentação uniforme em todos os serviços Java; métricas de negócio (receita total, latência ponta a ponta) junto com métricas técnicas
