### 1. Never Loose data

*  PostgreSQL or MySQL (configured for high availability)
   - Stricter ACID compliance by default. (Atomicity, Consistency, Isolation, Durability)
   - More robust WAL (Write-Ahead Logging). Recording all changes in a sequential log before applying them to the main data files.
   - PostgreSQL MVCC
      - PostgreSQL's MVCC is generally considered more robust and complete, offering greater ACID compliance and better performance in complex, write-heavy workloads, while MySQL's MVCC (via InnoDB) excels in read-                  intensive and simple applications but may have limitations in complex, high-concurrency scenarios compared to PostgreSQL.
   
   - Complex data types (JSON, arrays, hstore)
   - More powerful CTEs (Common Table Expressions) and window functions
   - Improved support for complex and analytical operations
   - Powerful extensions (PostGIS, pg_partman, etc.)

Scalability (Improved performance in complex operations):
   - Multiple and deep JOINs
   - Correlated subqueries
   - CTEs (Common Table Expressions)
   - Window functions
   - Complex aggregations (GROUP BY, HAVING)
   - Long and highly concurrent transactions


Why is PostgreSQL better at this?
   - More advanced query optimizer
   - PostgreSQL’s query planner is historically more sophisticated than MySQL’s.


PostgreSQL:
   - Evaluates multiple execution plans
   - Uses detailed statistics
   - Re-optimizes plans as data volume changes
   - Handles large and complex JOINs more efficiently

   MySQL:
   - Has improved significantly in recent years, but
   - Still struggles with very complex queries
   - May choose suboptimal execution plans for large JOINs

   In a voting system, this is critical for:
   - Real-time vote counting
   - Audits
   - Complex reports by region, time period, polling station, etc.
 

More efficient MVCC for concurrency
PostgreSQL implements MVCC (Multi-Version Concurrency Control) in a very mature way.

This means:
- Reads never block writes
- Writes never block reads
- High concurrency without performance degradation

During voting peaks:

- Thousands of votes are inserted simultaneously
- Users and systems read data at the same time
- Audits run in parallel
-- PostgreSQL maintains stable performance without lock contention.

MySQL (InnoDB):
- Also uses MVCC
- But can generate more contention under extreme workloads
- Locks appear more frequently at very high concurrency levels


# More advanced native partitioning (What is partitioning?)

Partitioning means splitting a very large table into smaller parts (partitions), for example:
   - By date
   - By region
   - By polling zone
   - By voter or ballot hash

This is essential when we have:
   - Billions of records
   - Continuous growth
   - Queries that always filter by some criteria

# Why is PostgreSQL’s partitioning superior?

Native and declarative partitioning
- Benefits:
   - The query planner is partition-aware
   - Automatic partition pruning
   - Only relevant partitions are accessed

- MySQL:
   - Partitioning exists, but it is more limited
   - Less flexible partition types
   - Less intelligent pruning for complex queries

Support for advanced partitioning strategies
- PostgreSQL natively supports:
   - RANGE
   - LIST
   - HASH
   - Subpartitioning (partitioning inside partitions)

This allows modeling the domain very precisely.
   Voting system example:
   - RANGE by date
   - LIST by state
   - HASH by polling station
   
> Result: faster queries and reduced I/O.
---
# Robust logical and physical replication

^This point is fundamental for a voting system.^

Physical replication (streaming replication)

PostgreSQL provides:
   - Binary replication at the WAL (Write-Ahead Log) level
   - Near real-time replicas
   - Reliable failover

Benefits:
   - Exact data copies
   - Zero data loss (when properly configured)
   - Ideal for high availability

MySQL:

- Uses binlog-based replication
- Works well, but:
   - More prone to inconsistencies
   - Longer history of replication lag issues

# Logical replication

PostgreSQL allows:
   - Replicating specific tables
   - Replicating only selected schemas
   - Evolving schemas without breaking replicas
   - Consuming changes as events (CDC – Change Data Capture)

This is excellent for:
   - Independent auditing
   - Parallel vote counting
   - Integration with external systems
   - Zero-downtime migrations

MySQL:
   - Logical replication is more limited
   - Less flexible for filtering and transformations
   - Schema evolution is more fragile

# Security and auditability
In a voting system:
   - You must prove that data has not been altered
   - You need to maintain audit trails
   - You need reproducibility

PostgreSQL:
   - WAL is extremely reliable
   - Strong support for auditing
   - Extensions such as pgAudit

^This carries significant weight in regulated systems.

-----
MySQL - Pros

- Simplicity:
   - Simpler configuration
   - Widely supported on hosting providers
   - Abundant documentation
   - Large community

- Performance:
   - Simple reads can be faster (MyISAM, but without ACID)
   - InnoDB has improved significantly in recent versions
   - Good for simple read-heavy applications

- MySQL - Cons
   - Integrity:
      - Historically less rigorous with data types
      - Silent conversions can cause data loss
      - Less reliable constraint implementation
      - Asynchronous replication by default (risk of data loss)

- Limitations:
   - Fewer advanced features
   - Less complete SQL implementation
   - Foreign keys only with InnoDB
 

# Conclusion
   We sohlud use PostgreSQL because:
   
   - Guaranteed durability - WAL and fsync ensure confirmed votes are never lost
   - Superior MVCC - Multiple users voting simultaneously without locks
   - Reliable transactions - Each vote is a complete ACID transaction
   - Strict constraints - Prevents invalid data (duplicate votes, incorrect values)
   - Auditing - Better support for triggers and change logging
   Conclusion for a voting system

PostgreSQL stands out because it:
   - Scales without compromising data integrity
   - Handles extremely high concurrency
   - Maintains strong consistency guarantees
   - Provides mature and reliable replication
   - Behaves more predictably under extreme load

That’s why, in scenarios such as:
   - Elections
   - Financial systems
   - Government registries
   - Immutable logging systems

^ PostgreSQL is generally the preferred choice over MySQL.

# Throughput

> the amount of material or items passing through a system or process.

### 4. Handle peak of 250k RPS

#### Option 1 - Highly Scalable Load Balancing + Horizontal Autoscaling (Kubernetes)

**Components**
* Edge Load Balancer (AWS ALB, NLB, Cloudflare)
    * ALB => Application Load Balancer
    * NLB => Network Load Balancer
* Kubernetes Deployment with Auto-scaling API instances (HPA + KEDA)
    * HPA => Horizontal Pod Autoscaler
        HPA increases or decreases the number of pod replicas using built-in metrics like:
            - CPU usage
            - Memory usage
            - (optionally) custom metrics
    * KEDA => Kubernetes Event-Driven Autoscaling
        Scale workloads based on event-driven metrics, not just CPU/Memory.

        KEDA allows Kubernetes to scale pods based on external system metrics:
            - Kafka topic lag (number of unprocessed messages)
            - Redis list length
            - HTTP RPS rate
            - Prometheus metrics
* Messaging layer (Kafka)
* Cache (Redis)

**How would work**
* Each API pod handles X RPS → you need 250K/X to have the number of pods needed
* Autoscaling based on metrics

**Pros**
* Elastic
* Simple

**Cons**
* Needs carefull tunned autoscaling configuration
* Auto scaling on spikes can be slower than needed (may need pre previsioning)
