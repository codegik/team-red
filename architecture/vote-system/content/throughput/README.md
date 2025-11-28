### 1. Never Loose data

> PostgreSQL or MySQL (configured for high availability)
> Write-Ahead Log (WAL)
   > Ensure the database is configured to enforce full durability on every write.
   > This means setting the fsync or equivalent parameter to guarantee data is written to non-volatile storage (disk) before acknowledging the transaction as complete.
   > synchronous replication
> Redundancy and High Availability
   > Deploy a database cluster across multiple physical machines and, ideally, multiple Availability Zones (AZs)
   > Synchronous Replication: All incoming votes are immediately copied to the replica nodes.
   > If the primary node fails, the replica already has all the data and can take over instantly (automatic failover).

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
