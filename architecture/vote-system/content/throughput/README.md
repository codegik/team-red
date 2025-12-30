### 1. Never Loose data

*  PostgreSQL or MySQL (configured for high availability)
- PostgreeSQL is the best option here, because:
     - Stricter ACID compliance by default. (Atomicity, Consistency, Isolation, Durability)
     - More robust WAL (Write-Ahead Logging). Recording all changes in a sequential log before applying them to the main data files.
     - PostgreSQL MVCC
         - PostgreSQL's MVCC is generally considered more robust and complete, offering greater ACID compliance and better performance in complex, write-heavy workloads, while MySQL's MVCC (via InnoDB) excels in read-                  intensive and simple applications but may have limitations in complex, high-concurrency scenarios compared to PostgreSQL.
- Advanced Features from PostGreSQL:
   - Complex data types (JSON, arrays, hstore)
   - More powerful CTEs (Common Table Expressions) and window functions
   - Improved support for complex and analytical operations
   - Powerful extensions (PostGIS, pg_partman, etc.)

- Scalability:
   - Improved performance in complex operations
   - More advanced native partitioning
   - Robust logical and physical replication
 

- PostgreSQL - Cons
   - More complex initial configuration
   - Requires more RAM
   - Slightly steeper learning curve
   - Less adoption in shared hosting environments

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
