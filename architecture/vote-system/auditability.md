# Immutable Audit Storage Architecture for Voting System
## Supporting 300M Users with PostgreSQL Sharding

---

## 1. Storage Solutions Comparison

### 1.1 S3 Object Lock vs Amazon QLDB

| Aspect | S3 Object Lock | Amazon QLDB |
|--------|----------------|-------------|
| **WORM Implementation** | Native Object Lock (Compliance/Governance mode) | Native append-only journal |
| **Hash Chains** | Must implement in application | Built-in (Merkle tree structure) |
| **Cryptographic Verification** | Must implement | Built-in digest verification |
| **Query Capability** | None (object storage) | PartiQL (SQL-like) queries |
| **Scalability** | Unlimited (S3 scale) | Limited to 10,000 req/sec per ledger |
| **Performance** | High throughput for writes | Lower throughput, optimized for consistency |
| **Cost Structure** | Storage + requests | Storage + I/O + read requests |

### 1.2 Cost Analysis (Estimates for 300M users)

**Assumptions:**
- 300M votes over 24 hours
- Average vote record: 1KB (JSON with hash chains)
- Retention: 10 years
- Total storage: ~300GB

#### S3 Object Lock Costs

```
Storage (S3 Standard):
- 300GB × $0.023/GB/month = $6.90/month
- Annual: $82.80

PUT Requests:
- 300M votes × $0.005/1000 = $1,500 (one-time)

GET Requests (auditing):
- Assuming 1% verification rate
- 3M reads × $0.0004/1000 = $1.20/month

Total First Year: ~$1,600
Total Annual (after year 1): ~$100
```

#### Amazon QLDB Costs

```
Storage:
- 300GB × $0.10/GB/month = $30/month
- Annual: $360

I/O Requests (Writes):
- 300M × $0.40/1M = $120

Read Requests (auditing):
- 3M reads × $0.15/1M = $0.45/month

Total First Year: ~$485
Total Annual (after year 1): ~$365
```

**Cost Verdict:** S3 is **~3-5x cheaper** than QLDB for this use case.

### 1.3 Tradeoffs Summary

#### Choose S3 Object Lock when:
✅ Cost is a primary concern  
✅ You need unlimited scale  
✅ Simple append-only audit log is sufficient  
✅ You can implement hash chains in application  
✅ You don't need to query the audit logs directly  

#### Choose QLDB when:
✅ You need built-in cryptographic verification  
✅ You want to query audit history with SQL-like syntax  
✅ You need automatic hash chain management  
✅ Budget allows for higher costs  
✅ Throughput requirements are < 10K req/sec  

---

## 2. Why NOT to Use PostgreSQL for WORM

### 2.1 PostgreSQL Limitations

PostgreSQL is a **transactional database**, not an immutable ledger. Key issues:

**Unrestricted Modifications:**
- Database administrators can UPDATE any record
- DELETE operations can remove audit evidence
- Entire tables can be dropped, including audit tables
- Triggers can be disabled to bypass audit mechanisms

**No Built-in WORM:**
- PostgreSQL is designed for CRUD operations (Create, Read, Update, Delete)
- The "UD" (Update, Delete) directly conflicts with WORM requirements
- No native immutability features

### 2.2 Attempted Solutions and Why They Fail

#### Attempt 1: Audit Tables with Triggers

**Approach:** Create a separate audit table that logs all changes via database triggers.

**Problems:**
- ❌ DBA can disable triggers
- ❌ DBA can delete audit table
- ❌ No cryptographic proof of integrity
- ❌ No WORM guarantee

#### Attempt 2: Revoke DELETE/UPDATE Permissions

**Approach:** Remove modification permissions from all users, including application users.

**Problems:**
- ❌ Superuser/owner can still modify
- ❌ Can drop and recreate table
- ❌ Physical file access on server
- ❌ Backup restoration can rollback state

#### Attempt 3: Use Write-Ahead Log (WAL)

**Approach:** Rely on PostgreSQL's write-ahead log for audit trail.

**Problems:**
- ❌ WAL is for crash recovery, not auditing
- ❌ WAL files are rotated/deleted
- ❌ Not designed for long-term retention
- ❌ Can be manipulated with physical access

### 2.3 Fundamental Issue

PostgreSQL's purpose is **mutable transactional data**, not immutability:

```
PostgreSQL Philosophy:
┌─────────────────────────────────────┐
│ ACID Transactions                   │
│ - Atomicity                         │
│ - Consistency                       │
│ - Isolation                         │
│ - Durability                        │
│                                     │
│ BUT: Allows modification by design  │
└─────────────────────────────────────┘

WORM Philosophy:
┌─────────────────────────────────────┐
│ Append-Only                         │
│ - Write once                        │
│ - Never modify                      │
│ - Never delete                      │
│ - Cryptographic proof               │
└─────────────────────────────────────┘
```

**Conclusion:** Use PostgreSQL for operational data, use dedicated WORM storage (S3/QLDB) for audit trail.

---

## 3. Datomic: The Immutable Database

### 3.1 Overview

Datomic is a database designed with **immutability as a core principle**, created by Rich Hickey (Clojure creator) and famously used by Nubank.

### 3.2 Key Characteristics

#### Immutability by Design

Datomic doesn't have traditional UPDATE or DELETE operations. Instead:

**Every "change" is a new fact added to history:**
- When you "update" a vote, you're actually adding a new fact to the database
- The old fact remains in history forever
- Both the original and "updated" values are preserved with timestamps

**Example scenario:**
- Time T1: Vote recorded for "Candidate A"
- Time T2: Vote "changed" to "Candidate B" (new fact added)
- History shows both facts with their respective timestamps
- You can query what the vote was at T1 or T2

**Benefits for auditing:**
- Complete historical record automatically maintained
- Cannot retroactively modify or delete past states
- Every change is timestamped and attributed
- Full audit trail without additional code

#### Time Travel Queries

Datomic allows you to query the database state at any point in the past:

- Query what the database looked like at a specific date/time
- See exactly what data existed at any moment in history
- Compare states across different time periods
- Critical for auditing: "Show me the state of all votes as of midnight on election day"

This is different from traditional databases where you only see the current state unless you've specifically built audit tables.

#### Architecture

```
┌──────────────────────────────────────┐
│         Transactor                   │
│  (Single write coordinator)          │
│  - Serializes all writes             │
│  - Creates immutable tx logs         │
└───────────────┬──────────────────────┘
                │
                ↓
┌──────────────────────────────────────┐
│         Storage Layer                │
│  (S3, DynamoDB, PostgreSQL, etc)     │
│  - Append-only segments              │
│  - Immutable by design               │
└───────────────┬──────────────────────┘
                │
                ↓
┌──────────────────────────────────────┐
│         Peer Nodes                   │
│  - Read directly from storage        │
│  - Local caching                     │
│  - No query load on transactor       │
└──────────────────────────────────────┘
```

### 3.3 Cost Analysis

#### Licensing Options

1. **Datomic Pro** (Self-hosted)
   - License: ~$5,000 - $20,000/year (based on cores)
   - Infrastructure costs on top
   - Full control

2. **Datomic Cloud** (AWS Marketplace)
   - Compute: Similar to EC2 instances
   - Storage: DynamoDB charges
   - Estimated for 300M users: **$5,000-15,000/month**

#### Cost Comparison (Annual)

```
S3 Object Lock:      ~$100/year
Amazon QLDB:         ~$365/year
Datomic Cloud:       ~$60,000-180,000/year
```

**Cost Verdict:** Datomic is **500-1800x more expensive** than S3.

### 3.4 Write Scalability Bottleneck

#### The Transactor Problem

```
Single Transactor Architecture:
┌─────────────────────────────────────┐
│  All writes must go through         │
│  ONE transactor instance             │
│                                     │
│  Write Capacity: ~10-20K tx/sec     │
│  (depends on transaction size)      │
└─────────────────────────────────────┘

For 300M votes in 24 hours:
- Required: 3,472 votes/sec average
- Peak (assuming 8-hour window): 10,417 votes/sec
```

**Analysis:**
- ✅ Average load: Within capacity
- ⚠️ Peak load: At the limit
- ❌ No horizontal scaling for writes
- ❌ Single point of failure

#### Workarounds

1. **Batch transactions** (reduces throughput)
2. **Multiple ledgers** (sharding, but loses single source of truth)
3. **Queue incoming votes** (adds latency)

### 3.5 When to Use Datomic

✅ **Good fit:**
- Financial transactions (like Nubank)
- Systems where complete audit history is critical
- When you need time-travel queries
- Smaller scale (< 1M tx/day)

❌ **Not ideal for:**
- High-throughput voting systems (300M users)
- Cost-sensitive applications
- Teams without Clojure/Java expertise

---

## 4. Recommended Architecture: PostgreSQL + S3 Hybrid

### 4.1 Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                    Vote Service (Microservice)                │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              Vote Processing Logic                      │  │
│  │                                                         │  │
│  │  1. Validate vote                                      │  │
│  │  2. Calculate hash chain                               │  │
│  │  3. Write to PostgreSQL (transactional)                │  │
│  │  4. Async write to S3 (audit trail)                    │  │
│  └────────────────────────────────────────────────────────┘  │
└───────────────────┬──────────────────┬───────────────────────┘
                    │                  │
         ┌──────────▼────────┐    ┌───▼──────────────────────┐
         │   PostgreSQL      │    │   Message Queue          │
         │   (Sharded)       │    │   (Kafka/RabbitMQ/SQS)   │
         │                   │    │                          │
         │  - Operational    │    └───┬──────────────────────┘
         │    queries        │        │
         │  - Fast reads     │        │
         │  - Sharded by     │        ▼
         │    region/voter   │    ┌──────────────────────────┐
         └───────────────────┘    │  Audit Writer Service    │
                                  │                          │
                                  │  - Consume from queue    │
                                  │  - Batch writes to S3    │
                                  │  - Checkpoint hashes     │
                                  └───┬──────────────────────┘
                                      │
                                      ▼
                              ┌────────────────────────────┐
                              │   S3 Object Lock (WORM)    │
                              │                            │
                              │   Bucket Structure:        │
                              │   /votes/                  │
                              │     checkpoint_0001/       │
                              │       vote_0000001.json    │
                              │       vote_0000002.json    │
                              │       ...                  │
                              │     checkpoint_0002/       │
                              │       checkpoint.json      │
                              │       vote_0010001.json    │
                              └────────────────────────────┘
```

### 4.2 Detailed Component Design

#### 4.2.1 PostgreSQL Sharding Strategy

**Sharding Key Options:**

**Option 1: Shard by Region** (if voting is geographically distributed)
- Create separate tables for each geographic region (US_EAST, US_WEST, EU, etc.)
- Simple routing based on voter location
- Good for compliance requirements with data locality

**Option 2: Shard by voter_id hash** (uniform distribution)
- Hash the voter_id and use modulo operation to determine shard
- Creates uniform distribution across shards
- Example: shard_id = hash(voter_id) % 16
- Better load balancing across all shards

**Recommended:** 16 shards initially, can increase as needed based on load patterns.

#### 4.2.2 Vote Service Implementation

**Core Responsibilities:**
1. **Vote validation** - Ensure voter hasn't already voted
2. **Shard determination** - Calculate which PostgreSQL shard to use based on voter_id hash
3. **Transactional write** - Insert vote into appropriate PostgreSQL shard
4. **Hash chain calculation** - Retrieve previous hash and calculate current hash
5. **Async audit publishing** - Send audit log to message queue for S3 persistence

**Hash Chain Logic:**
- Retrieve the hash of the previous vote (from cache or S3)
- Calculate current hash: SHA256(prev_hash + vote_id + voter_id + candidate_id + timestamp)
- This creates a tamper-evident chain where any modification breaks the sequence

**Error Handling:**
- PostgreSQL write failure → Vote is rejected, return error to client
- Message queue publish failure → Log error but don't fail the vote (will be retried)
- This ensures votes are never lost due to audit system issues

#### 4.2.3 Audit Writer Service (S3 Consumer)

**Core Responsibilities:**
1. **Consume from message queue** - Read audit log messages from Kafka/RabbitMQ/SQS
2. **Write to S3 with Object Lock** - Persist audit logs as immutable objects
3. **Create checkpoints** - Every 10,000 votes, create a checkpoint file with cumulative hash
4. **Retry logic** - Handle failures and retry S3 writes to ensure no audit logs are lost

**S3 Organization Structure:**
```
s3://voting-audit-bucket/
  votes/
    checkpoint_0000000001/
      vote_0000000001.json
      vote_0000000002.json
      ...
      vote_0000010000.json
      checkpoint.json         ← Summary with final hash
    checkpoint_0000000002/
      vote_0000010001.json
      ...
```

**Object Lock Configuration:**
- Mode: COMPLIANCE (cannot be deleted even by root account)
- Retention: 10 years from creation date
- Ensures true WORM behavior

**Checkpoint Files:**
- Created every 10,000 votes
- Contains: checkpoint_id, last_vote_id, last_hash, timestamp
- Enables fast verification without reading all individual vote files
- Can be digitally signed for additional security

#### 4.2.4 S3 Bucket Configuration

**Required Settings:**

**1. Object Lock Enabled**
- Must be enabled at bucket creation (cannot be added later)
- Provides WORM capability

**2. Versioning Enabled**
- Required for Object Lock to work
- Maintains all versions of objects

**3. Object Lock Configuration**
- Mode: COMPLIANCE (strongest protection - even root cannot delete)
- Default Retention: 10 years
- Alternative: GOVERNANCE mode (allows privileged users to override)

**4. Lifecycle Policies**
- Transition to Glacier after 90 days to reduce costs
- S3 Standard: $0.023/GB/month
- Glacier: $0.004/GB/month (saves ~83%)
- Objects remain accessible but with longer retrieval time

**Cost Optimization:**
- First 90 days: S3 Standard (fast access for recent audits)
- After 90 days: Glacier (cold storage for compliance)
- Deep Archive option: $0.00099/GB/month for long-term archival

### 4.3 Data Flow

#### Write Path (Synchronous)

```
1. Client → Vote Service
   └─ POST /api/v1/votes
      {
        "voter_id": "voter_abc123",
        "candidate_id": "candidate_xyz"
      }

2. Vote Service:
   ├─ Validate voter (not duplicate)
   ├─ Determine shard: hash(voter_id) % 16 = shard_7
   ├─ PostgreSQL INSERT into votes_shard_7
   │  └─ Returns vote.id = 12345678
   ├─ Get prev_hash from Redis/S3
   ├─ Calculate current_hash
   └─ Publish to Kafka topic "audit-logs"

3. Response to Client:
   └─ 201 Created
      {
        "vote_id": 12345678,
        "status": "recorded"
      }
   
   Duration: ~50-100ms
```

#### Audit Path (Asynchronous)

```
1. Kafka Consumer (Audit Writer Service):
   ├─ Consume message from "audit-logs"
   ├─ Prepare JSON with hash chain
   └─ S3 PUT with Object Lock
      └─ Key: votes/checkpoint_0001234/vote_0012345678.json

2. Every 10,000 votes:
   └─ Create checkpoint file
      └─ Key: votes/checkpoint_0001234/checkpoint.json

Duration: ~500ms-2s (async, doesn't block vote)
```

### 4.4 Verification Process

**Hash Chain Integrity Verification:**

The verification process ensures no tampering has occurred by validating the entire hash chain:

1. **Retrieve all votes in a checkpoint** - List all vote files from S3 for a given checkpoint
2. **Sort chronologically** - Order votes by their ID to follow the chain sequence
3. **Verify each link** - For each vote, confirm:
   - The prev_hash matches the previous vote's current_hash
   - Recalculating the current_hash produces the same result
4. **Detect tampering** - Any modification to vote data or hash values will break the chain

**Types of Verification:**

**Real-time Verification:**
- Performed during vote submission
- Validates the last N votes in the chain
- Catches issues immediately

**Batch Verification:**
- Run periodically (e.g., end of each checkpoint)
- Validates entire checkpoint from first to last vote
- Confirms no retroactive tampering

**Full Audit:**
- Validates the complete voting history
- Can be performed by independent auditors
- Checkpoint files enable faster validation by verifying checkpoint summaries first

**Public Verifiability:**
- Checkpoint digests can be published publicly
- Third parties can verify without accessing individual votes
- Enables transparent auditing while protecting voter privacy

### 4.5 Why This Architecture?

✅ **Separation of Concerns:**
- PostgreSQL: Fast operational queries, sharded for scale
- S3: Immutable audit trail, cryptographic proof
- Kafka: Decouples write paths, enables retries

✅ **Scalability:**
- PostgreSQL sharding: 16 shards × 10K writes/sec = 160K votes/sec
- S3: Unlimited storage, high throughput
- Kafka: Buffer for traffic spikes

✅ **Cost-Effective:**
- PostgreSQL: Standard RDS pricing
- S3: ~$100/year for 300M votes
- Kafka: Managed service (MSK) or self-hosted

✅ **Reliability:**
- Async S3 writes don't block votes
- Kafka provides durability and retries
- Object Lock prevents tampering

✅ **No Lambda Required:**
- Dedicated microservices for control
- Easier debugging and monitoring
- Can deploy in Kubernetes/ECS

---

## 5. Summary and Recommendations

### For a 300M User Voting System:

**✅ Recommended: PostgreSQL (sharded) + S3 Object Lock**

- **PostgreSQL:** Operational data, fast queries, proven scalability
- **S3 Object Lock:** Immutable audit trail, lowest cost, unlimited scale
- **Message Queue:** Decouple write paths, enable async processing
- **Microservices:** Full control, no Lambda cold starts

**Cost:** ~$100-500/year for audit storage (vs. $60K+ for Datomic)

**Why NOT QLDB:** 3-5x more expensive, throughput limits

**Why NOT Datomic:** 500-1800x more expensive, write bottleneck, overkill

**Why NOT PostgreSQL alone:** No true WORM, admin can tamper

---

## 6. Next Steps

1. **Prototype the Vote Service** with hash chain implementation
2. **Set up PostgreSQL sharding** (start with 4-8 shards)
3. **Configure S3 bucket** with Object Lock in Compliance mode
4. **Implement Kafka pipeline** for async audit writes
5. **Build verification tools** to validate hash chains
6. **Load test** the architecture (simulate 10K votes/sec)
7. **Disaster recovery plan** for S3 and PostgreSQL

---

**Document Version:** 1.0  
**Last Updated:** January 2026  
**Author:** Technical Architecture Team