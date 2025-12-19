### 1. Never Loose data

Data durability is the result of multiple layers working together:

- Correct transactional data modeling
- A reliable, ACID-compliant database
- Safe write configuration
- Proper replication strategy
- Tested and reliable backups
- Redundant infrastructure
- Well-defined operational procedures

If any of these layers fail, votes can be lost.

---

## 2. Database Choice: PostgreSQL vs MySQL

### ✅ Recommended Database: **PostgreSQL**

PostgreSQL is widely used in **financial, governmental, and electoral systems** due to its strong guarantees around consistency and durability.

| Criterion | PostgreSQL | MySQL |
|---------|------------|-------|
| ACID compliance | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Complex transactions | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Strong consistency | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| WAL robustness | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Synchronous replication | ⭐⭐⭐⭐ | ⭐⭐ |
| Auditing capabilities | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Use in critical systems | Banks, governments | Web apps |

### When MySQL Might Be Acceptable
- Simple applications
- Read-heavy workloads
- Systems where limited data loss is acceptable (**not suitable for voting**)

## 3. Write-Ahead Logging (WAL)

### Mandatory: **YES**

Write-Ahead Logging ensures that:

- Every change is written to a log **before** being committed
- If a crash occurs, the database can **replay committed transactions**

PostgreSQL includes WAL by default, but it must be **properly configured**.

### Critical PostgreSQL Settings
```conf
synchronous_commit = on
wal_sync_method = fdatasync
full_page_writes = on
```

### 4. Synchronous Replication

Synchronous replication ensures that:

A transaction is only committed after:

The primary node writes it

At least one standby node confirms the write

If the primary crashes immediately after a vote, the vote still exists on the replica.

## Trade-offs
| Pros | Cons | 
|---------|------------|
| Zero data loss	| Higher latency | 
Strong durability	| Slower writes | 
Ideal for voting	| Requires reliable network | 

---

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
