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
