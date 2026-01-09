# Handle 300M users

To support 300M users, we need to worry about various critical aspects, and the Database is perhaps the major challenge.

In my searches, all recommendations about databases are to use Shards. But, what is database sharding and why do we need it?

Database sharding is a process to split data across multiple database machines. 

A single machine, or database server, can store and process only a limited amount of data. So, the database sharding is used to solve this.

As an application grows, the number of application users and amount of data it stores increase over time. The database becomes a bottleneck if the data volume becomes too large and too many users attempt to use the application to read or save information simultaneously. The application slows down and affects customer experience. Database sharding is one of the methods to solve this problem because it enables parallel processing of smaller datasets across shards.

So, to support 300M users, the database sharding is REQUIRED.

## Method of the database sharding

There are many methods of database sharding. For our scenario, the our choice was Geo Sharding. Geo sharding splits and stores database information according to geographical location. For example, a dating service website uses a database to store customer information from various cities as follows. 

Reference: https://aws.amazon.com/what-is/database-sharding/?nc1=h_ls


# Trade-offs

## SQL vs No-SQL

| **Criteria**                   | **SQL Databases**                                                                   |**No-SQL databases**                                       | **Notes** |
|------------------------------|---------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|-----------|
| **Consistency** | Strong (ACID)    | Eventual (in most cases)                                 | **SQL**: Guarantees ACID properties (Atomicity, Consistency, Isolation, Durability). Changes are immediately visible to all users after commit. Critical for voting systems where vote counts must be accurate in real-time. **NoSQL**: Most NoSQL databases prioritize availability and partition tolerance (CAP theorem), using eventual consistency where changes propagate over time. Cassandra, DynamoDB offer tunable consistency but at performance cost. |
| **Scalability** |  Mainly vertical                     | Horizontal | **SQL**: Scales by adding more CPU, RAM, or storage to a single server (vertical scaling). Sharding is possible but complex. **NoSQL**: Designed for horizontal scaling - adding more servers to distribute load. Handles massive data volumes across distributed nodes naturally. |
| **Schema**       | Rigid | Flexible                  | **SQL**: Requires predefined schema with strict data types and relationships. Changes require migrations (ALTER TABLE). Ensures data integrity but less adaptable. **NoSQL**: Schema-less or flexible schema allows storing varied document structures. Easy to add new fields without migrations, but application must handle data validation. |
| **Transaction**  | Complex and fully supported                 | Limited           | **SQL**: Full ACID transaction support across multiple tables with JOIN operations, foreign keys, and complex queries. Can handle multi-step operations atomically. **NoSQL**: Most support single-document/row transactions only. Some (MongoDB, DynamoDB) added multi-document transactions but with performance penalties. Complex transactions require application-level coordination. |
| **Complexity**   | Lower for relational data                                  |    Higher at the application level                   | **SQL**: Database handles relationships, constraints, and data integrity. Application code is simpler for relational data. Mature tooling and widespread expertise. **NoSQL**: Application must handle data relationships, consistency checks, and complex queries. Requires careful data modeling and denormalization strategies. |
| **Performance**     | Good for transactional workloads           | Excelent for high data volume                        | **SQL**: Optimized for complex queries with JOINs, aggregations, and ACID transactions. Performance degrades with very large datasets without sharding. **NoSQL**: Excels at simple read/write operations on massive datasets. Optimized for high throughput and low latency at scale. Less efficient for complex analytical queries. |

## Pros

- High horizontal scalability
- Schema flexibility
- Better performance for large data volumes
- High availability
- Ideal for distributed workloads

## Cons

- Eventual consistency (in most cases)
- Less support for complex transactions
- Complex queries can be more difficult
- Greater application responsibility for data consistency

## NoSQL or SQL?

In my searches, all recommendations for voting systems are to use SQL databases. 

Why Not NoSQL for Vote System?
* NoSQL lacks strong ACID guarantees by default.
* Even with configurable consistency (e.g., quorum reads/writes in Cassandra), you’re trading performance for complexity.
* For mission-critical voting paths, use PostgreSQL or MySQL with strict isolation (Serializable level).

## Use NoSQL for:

* Logs (vote_cast events)
* Audit trails
* High-throughput analytics pipelines


So, our choice for the database is Amazon RDS Postgresql

## Auditability and Tamper-Proofing

In voting systems, it is very important to guarantee that nothing can be changed, deleted or modified. So, for this purpose, we have the following suggestion:

### AWS OpenSearch

https://docs.opensearch.org/latest/install-and-configure/configuring-opensearch/security-settings/

plugins.security.compliance.immutable_indices (Static): Documents in indexes that are marked as immutable follow the write-once, read-many paradigm (WORM). Documents created in these indexes cannot be changed and are therefore immutable.

## Redundancy, Backups and Disaster Recovery

We need to plan and study more about it, because it's so important.