## pgsync

**pgsync** is an open-source tool for **synchronizing PostgreSQL data to Elasticsearch** (or OpenSearch) in **real time**. It’s ideal for building fast, full-text search capabilities on top of PostgreSQL databases without duplicating infrastructure or overloading your primary database.

### Key Features

* 🔄 **Real-time Sync**: Streams **PostgreSQL changes (CDC)** using the **logical replication** feature.
* 🔍 **Elasticsearch Integration**: Pushes structured or nested documents directly to **Elasticsearch/OpenSearch** indices.
* ⚙️ **Custom Mappings**: Define **JSON-based mapping templates** to transform SQL rows into searchable documents.
* 📊 **Selective Syncing**: Sync specific tables, fields, or related rows using **joins and filters**.
* 📥 **Initial and Continuous Sync**: Runs a one-time full sync and then switches to live updates.
* 🧩 **Flexible Config**: Simple YAML config to define sources, targets, transformations, and filtering.
* 🛡️ **Safe and Lightweight**: Designed to have **minimal impact** on your PostgreSQL performance.
* 🔌 **Docker-friendly**: Easy deployment via Docker for local or production environments.

### Common Use Cases

* 🔎 **Full-text search** in applications that store primary data in PostgreSQL.
* 🚀 **Real-time analytics dashboards** needing fast, indexed querying.
* 🧩 **Microservices** that require a search-optimized data layer separate from the main DB.
* 🛠️ **Decoupled search layers** where search logic is handled independently from the transactional DB.
* 📉 **Avoiding polling or batch jobs** by reacting to real-time DB changes.

👉 In short: **pgsync bridges PostgreSQL and Elasticsearch with real-time, configurable syncing — enabling powerful search features without complex ETL pipelines.**
