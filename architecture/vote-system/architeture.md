# 🧬 Realtime Voting System -- Architecture Overview

# 1. 🏛️ Structure

## 1.1 🎯 Problem Statement and Context

This document describes the high-level architecture and security
strategy for a global, real-time voting system designed to support:

- Up to 300 million registered users
- Traffic peaks of 240,000 requests per second (RPS)
- Strict consistency and reliability guarantees
- Strong protection against bots, fraud, and abuse
- One-person-one-vote enforcement
- Near real-time result visibility

The system must be fully cloud-native, highly scalable, fault tolerant,
and secure by design, while explicitly avoiding:

## 1.2 Restrictions

- Serverless platforms outside AWS
- MongoDB
- On-premise infrastructure
- Google Cloud & Microsoft Azure
- OpenShift
- Mainframes
- Monolithic architectures

AWS-based, fully distributed, microservices-first architecture is
assumed.

## 1.3 Problem Space

**What is the problem?**

We need to design and build a globally distributed, mission-critical real-time voting system capable of handling 300 million registered users with peak traffic of 240,000 requests per second. The system must guarantee absolute data integrity, enforce strict one-person-one-vote constraints, provide real-time result visibility, and defend against sophisticated fraud, bot attacks, and abuse at scale—all while maintaining near-zero data loss and high availability across multiple geographic regions.

**What is the context of the problem?**

- **Market Context**: 
  - Democratic elections and large-scale voting events demand unprecedented levels of trust, transparency, and reliability
  - Growing threat landscape from automated bots, state-sponsored actors, and coordinated fraud campaigns
  - Increasing expectations for instant feedback and real-time results from 300M+ global participants
  - Zero tolerance for data loss, system failures, or security breaches that could undermine election integrity
  - Need for systems that can scale elastically during unpredictable traffic spikes (campaigns, debates, breaking news)

- **Business Context**: 
  - Any data loss or security breach creates legal liability, regulatory penalties, and irreparable reputational damage
  - System must support mission-critical operations with financial and legal consequences
  - One-time deployment windows with no room for failure during live voting periods
  - Requirement for complete auditability and tamper-proof logging for legal compliance
  - Cost optimization critical—infrastructure must scale down after peak periods
  - Must support strict SLAs with penalties for downtime or data inconsistency

- **Technical Context**: 
  - Peak traffic of 240K RPS eliminates traditional vertical scaling and monolithic architectures
  - 300M users require geo-distributed sharding, multi-region replication, and CDN distribution
  - ACID guarantees required for vote integrity—NoSQL eventually consistent models insufficient for vote records
  - Real-time requirements demand event-driven architecture (Kafka, SSE) with <2 second latency
  - Cloud-native AWS-only restriction eliminates multi-cloud and serverless options
  - Must handle database bottlenecks through strategic sharding (geo-based), read replicas, and caching
  - Defense-in-depth security model required across network, identity, device, behavior, application, and data layers
  - Need for immutable audit logs and write-once-read-many (WORM) compliance

- **User Context**:
  - 300M users spread across multiple geographic regions with varying network conditions
  - Users expect instant confirmation of vote submission and real-time result updates
  - Users must be authenticated securely without friction (prevent credential stuffing, session hijacking)
  - Mobile and web clients require Server-Sent Events (SSE) for efficient real-time updates
  - Users in different time zones create distributed load patterns with unpredictable spikes
  - Accessibility requirements for diverse user populations (language, disability, device types)
  - Users must vote exactly once—any duplicate vote undermines system integrity

**Core Challenges:**

1. **Data Integrity at Scale**
   - Guarantee zero data loss across 240K RPS with multi-region active-active replication
   - Implement synchronous writes with WAL (Write-Ahead Logging) and fsync guarantees
   - Build tamper-proof immutable audit trails using OpenSearch WORM indices
   - Handle database sharding across 300M users without creating consistency gaps
   - Design automatic failover with RPO=0 (Recovery Point Objective) and RTO<60s (Recovery Time Objective)

2. **Security & Fraud Prevention**
   - Detect and block automated bots at 240K RPS without impacting legitimate users
   - Prevent credential stuffing, session hijacking, replay attacks, and DDoS
   - Implement defense-in-depth: WAF, device fingerprinting, behavioral analysis, rate limiting
   - Validate identity uniqueness across 300M users without centralized bottlenecks
   - Build real-time fraud detection with ML models analyzing voting patterns

3. **Horizontal Scalability**
   - Scale API layer from baseline to 240K RPS using Kubernetes HPA and KEDA
   - Implement geo-based database sharding to distribute 300M user records
   - Design stateless microservices that auto-scale without session affinity issues
   - Optimize cache layers (Redis) to absorb read-heavy traffic and reduce DB load
   - Handle cold-start delays during sudden traffic spikes with pre-warming strategies

4. **Strict Idempotency & One-Vote Enforcement**
   - Guarantee exactly-once vote processing despite retries, network failures, and race conditions
   - Implement distributed locks or optimistic concurrency control at database level
   - Design idempotency keys with conflict resolution for duplicate submissions
   - Prevent race conditions when multiple requests arrive simultaneously for same user
   - Build reconciliation mechanisms to detect and resolve any duplicate votes in audit logs

5. **Real-Time Result Distribution**
   - Stream aggregated results to 300M users with <2 second latency using SSE
   - Design event-driven architecture (Kafka) for vote ingestion and aggregation
   - Handle 250K concurrent SSE connections with minimal server resource overhead
   - Implement efficient broadcast patterns using EventEmitter for SSE clients
   - Balance real-time updates with system load—aggregate summaries vs. individual events

6. **Multi-Region Complexity**
   - Synchronize vote data across geographic regions with strong consistency
   - Handle network partitions (split-brain scenarios) without duplicate votes
   - Route users to nearest region while maintaining global vote count accuracy
   - Implement cross-region disaster recovery with automated failover
   - Manage clock skew and distributed transaction coordination across regions

7. **Performance Under Load**
   - Maintain <100ms p99 latency during 240K RPS peak traffic
   - Prevent database saturation through write buffering, connection pooling, and read replicas
   - Optimize Kafka throughput for event streaming without lag buildup
   - Implement backpressure mechanisms to gracefully degrade under extreme load
   - Cache authentication tokens and session data to reduce repeated DB lookups

---

# 2. 🎯 Goals

## 2.1 Never Lose Data

Voting systems are mission-critical. Any data loss leads to: - Legal
risks - Loss of public trust - Invalid election outcomes

This requires: - Multi-region replication - Strong durability
guarantees - Strict write acknowledgements - Immutable audit logs

---

## 2.2 Be Secure and Prevent Bots & Bad Actors (Primary Ownership Area)

This is one of the hardest challenges at global scale. The system must
prevent:

- Automated voting (bots)
- Credential stuffing
- Distributed fraud attacks
- Replay attacks
- Session hijacking
- API scraping
- DDoS attacks

Security must be implemented in multiple layers (defense in depth): -
Network - Identity - Device - Behavior - Application - Data

---

## 2.3 Handle 300M Users

This implies: - Massive horizontal scalability - Stateless
architectures - Global CDNs - Partitioned databases - Multi-region
deployment

---

## 2.4 Handle 240K RPS Peak Traffic

This eliminates: - Vertical scaling - Centralized bottlenecks - Stateful
monoliths

It requires: - Load-based autoscaling - Event-driven processing -
Front-door traffic absorption - Backpressure handling

---

## 2.5 One Vote per User (Strict Idempotency)

This is a data + security + consistency problem: - Each identity must
be: - Verified - Unique - Non-replayable - Vote submissions must be: -
Idempotent - Conflict-safe - Race-condition proof

---

## 2.6 Real-Time Results

This creates challenges in: - Data streaming - Cache invalidation -
Broadcast consistency - Fan-out architectures - WebSocket / pub-sub
scalability

---

# 3. 🎯 Non-Goals

- On-prem or hybrid operation
- Manual moderation for fraud detection
- Single-region deployment
- Strong coupling between frontend and backend

---

# 4. 📐 Design Principles

The architecture is guided by seven foundational design principles that address the unique challenges of building a mission-critical, globally distributed voting system. These principles inform every architectural decision, from technology selection to deployment strategies.

---

## 4.1 Security First (Defense in Depth)

**Principle**: Security is not a feature—it's the foundation. Every layer of the system must assume breach and implement independent security controls.

**Why This Matters**:
- Voting systems are high-value targets for nation-state actors, organized fraud, and automated bot armies
- A single security failure can compromise election integrity and destroy public trust
- Attack vectors evolve constantly—security must be layered and adaptive

**Implementation Strategy**:

### Layer 1: Network & Edge Security
- **AWS WAF**: Block common attack patterns (SQL injection, XSS, CSRF)
- **DDoS Protection**: AWS Shield Advanced for volumetric attack mitigation
- **Geographic Filtering**: Route53 + CloudFront geo-restrictions to block suspicious regions
- **Rate Limiting**: Token bucket algorithm at edge to prevent request flooding

### Layer 2: Identity & Authentication
- **Auth0 SSO + MFA**: Multi-factor authentication (SMS, authenticator apps, push notifications)
- **Liveness Detection**: SumSub facial biometrics to prevent fake accounts and deepfakes
- **Document Verification**: Government ID validation with fraud risk scoring
- **Session Binding**: Tokens tied to device fingerprint and IP address

### Layer 3: Device Intelligence
- **FingerprintJS**: Device fingerprinting to detect emulators, VMs, and bot farms
- **Jailbreak/Root Detection**: Block compromised devices
- **Behavioral Biometrics**: Analyze touch patterns, typing speed, mouse movements
- **Challenge-Response**: Cloudflare Turnstile for invisible human verification

### Layer 4: Application Security
- **OAuth2 Bearer Tokens**: Short-lived access tokens (15 min TTL) with secure refresh flows
- **API Gateway**: AWS API Gateway with request validation and transformation
- **Input Sanitization**: Strict schema validation on all API requests
- **HTTPS Everywhere**: TLS 1.3 with certificate pinning on mobile clients

### Layer 5: Data Protection
- **Encryption at Rest**: AES-256 for database, S3, and backups
- **Encryption in Transit**: TLS 1.3 for all service-to-service communication
- **Field-Level Encryption**: Sensitive PII encrypted at application layer
- **Key Rotation**: Automated rotation via AWS KMS with audit trails

### Layer 6: Audit & Monitoring
- **Immutable Logs**: OpenSearch with WORM (Write-Once-Read-Many) indices
- **Real-Time Anomaly Detection**: Machine learning models flagging suspicious voting patterns
- **SIEM Integration**: AWS Security Hub aggregating security events
- **Forensic Readiness**: Complete audit trail for post-incident investigation

**Trade-offs**:
- Increased latency from security checks at each layer
- Higher infrastructure costs from redundant security systems
- Potential false positives requiring manual review workflows

**Success Metrics**:
- Zero successful bot votes detected in production
- <0.01% false positive rate on fraud detection
- 100% of attacks blocked at edge (no malicious traffic reaches application layer)

---

## 4.2 Scalability by Default (Horizontal, Stateless, Elastic)

**Principle**: The system must scale horizontally without architectural changes. Every component is designed for elastic scaling from day one.

**Why This Matters**:
- Peak traffic (240K RPS) is 100x baseline load—vertical scaling is impossible
- Voting events create unpredictable traffic spikes (debates, breaking news, election day)
- Cost optimization requires scaling down after peak periods

**Implementation Strategy**:

### Stateless API Layer
- **Kubernetes Deployments**: Pods are ephemeral, interchangeable, and horizontally scalable
- **No In-Memory State**: Session data stored in Redis, not application memory
- **Container Images**: Immutable Docker images with health checks for fast startup
- **Auto-Scaling Policies**:
  - **HPA (Horizontal Pod Autoscaler)**: Scale based on CPU and memory
  - **KEDA (Kubernetes Event-Driven Autoscaling)**: Scale based on Kafka lag, Redis queue depth, custom Prometheus metrics

### Database Sharding Strategy
- **Geo-Based Sharding**: Users partitioned by geographic region (North America, Europe, Asia-Pacific)
- **Consistent Hashing**: User ID hashed to determine shard assignment
- **Read Replicas**: 3-5 read replicas per shard to distribute query load

### Event-Driven Asynchronous Processing
- **Kafka Partitioning**: 50+ partitions per topic for parallel consumption
- **Consumer Groups**: Multiple consumer instances per group for horizontal scaling
- **Backpressure Handling**: Kafka provides natural buffering during load spikes

**Trade-offs**:
- Cold-start delays (30-60s) when scaling from zero
- Higher infrastructure complexity with sharding and caching
- Eventual consistency in cached data (2-second lag on vote counts)

**Success Metrics**:
- Scale from 1K to 240K RPS in <5 minutes
- Maintain <100ms p99 API latency during scale-up
- Auto-scale down to baseline within 10 minutes after traffic drops

---

## 4.3 Event-Driven Architecture (Asynchronous, Decoupled, Resilient)

**Principle**: Decouple producers and consumers through event streams. All critical operations are asynchronous to prevent cascading failures.

**Why This Matters**:
- Synchronous request-response patterns create tight coupling and single points of failure
- Database writes at 240K RPS would saturate any relational database
- Real-time result aggregation requires parallel event processing

**Implementation Strategy**:

### Kafka as Central Event Bus
- **Vote Submission Topic**: All votes published as events (producer: API layer)
- **Vote Aggregation Topic**: Processed vote summaries (producer: aggregation service)
- **Audit Log Topic**: Immutable event stream for compliance
- **Replication Factor**: 3 (across availability zones for durability)


**Trade-offs**:
- Eventual consistency—vote confirmation may take 500ms to 2 seconds
- Increased operational complexity (Kafka cluster management)
- Debugging asynchronous failures is harder than synchronous flows

**Success Metrics**:
- 99.99% of events processed within 2 seconds
- Zero message loss (exactly-once semantics)
- Kafka lag <1000 messages during peak load

---

## 4.4 Stateless Compute (Immutable, Ephemeral, Replaceable)

**Principle**: Application servers hold no persistent state. Every instance is interchangeable and can be destroyed/recreated without data loss.

**Why This Matters**:
- Stateful servers cannot scale horizontally (sticky sessions create hotspots)
- Server failures with in-memory state cause data loss
- Rolling updates and auto-scaling require killing instances without warning

**Implementation Strategy**:

### Session State Externalization
- **Redis for Sessions**: All session data stored in distributed Redis cluster
- **JWT Tokens**: Stateless authentication—server validates signature without DB lookup
- **Sticky Session Elimination**: Load balancer distributes requests randomly

### Immutable Infrastructure
- **No SSH Access**: Servers are never modified after deployment
- **Configuration via Environment Variables**: All config injected at container startup
- **Blue-Green Deployments**: New versions deployed alongside old, traffic switched atomically

### Health Checks & Auto-Recovery
- **Kubernetes Liveness Probes**: Kill and restart unhealthy pods
- **Readiness Probes**: Remove pods from load balancer if not ready
- **Pod Disruption Budgets**: Ensure minimum replicas during voluntary disruptions

**Trade-offs**:
- Redis becomes critical dependency (must be highly available)
- Cannot use in-memory caching—must use distributed cache
- Slightly higher latency (network hop to Redis for every request)

**Success Metrics**:
- 100% of requests succeed even when 50% of pods are terminated
- Zero data loss during rolling updates
- Mean time to recovery (MTTR) <30 seconds

---

## 4.5 Multi-Layer Anti-Abuse Protection (Adaptive, ML-Driven, Zero Trust)

**Principle**: Assume every request is malicious until proven otherwise. Defense mechanisms adapt in real-time to emerging threats.

**Why This Matters**:
- Bots evolve to bypass static rules (CAPTCHA solving, residential proxies)
- Credential stuffing attacks leverage millions of stolen username/password pairs
- Distributed attacks from 100K+ IP addresses bypass simple rate limiting

**Implementation Strategy**:

### Static Defenses (Always Active)
- **Rate Limiting**: 10 requests/second per IP, 1 vote/user/election
- **IP Reputation**: Block known proxy/VPN/Tor exit nodes
- **Geo-Fencing**: Restrict voting to eligible geographic regions
- **User-Agent Validation**: Block non-standard or suspicious user agents

### Dynamic Defenses (ML-Powered)
- **Behavioral Analysis**:
  - Typing speed anomalies (too fast = bot, too slow = automation)
  - Mouse movement patterns (linear paths = automation)
  - Session duration (instant submission = bot)
- **Anomaly Detection Models**:
  - Train on legitimate user baselines
  - Flag deviations >3 standard deviations
  - Real-time scoring with SageMaker inference endpoints
- **Graph Analysis**:
  - Detect coordinated voting rings (same IP block, timing patterns)
  - Identify bot clusters by behavioral similarity

### Adaptive Challenge Escalation
```
Low Risk: No challenge
Medium Risk: Cloudflare Turnstile (invisible)
High Risk: SumSub liveness re-verification
Critical Risk: Manual review queue
```

**Trade-offs**:
- False positives frustrate legitimate users
- ML model training requires large labeled datasets
- Real-time inference adds latency

**Success Metrics**:
- Block 99.9% of bot traffic without human intervention
- False positive rate <0.1% (1 in 1000 legitimate users challenged)
- Detect novel attack patterns within 5 minutes

---

## 4.6 Auditable Data (Immutable, Tamper-Proof, Forensic-Ready)

**Principle**: Every vote and system action is recorded in an append-only, cryptographically verifiable audit log.

**Why This Matters**:
- Legal requirements for election audits and recounts
- Post-incident forensics require complete event reconstruction
- Public trust depends on transparent, verifiable vote counting

---

## 4.7 Failure as a Normal Condition (Chaos Engineering, Graceful Degradation)

**Principle**: Expect failures at every level. Design systems that degrade gracefully and self-heal automatically.

**Why This Matters**:
- At 240K RPS, component failures are guaranteed (hardware, network, software bugs)
- Manual intervention is too slow—recovery must be automatic
- Partial availability is better than complete outage

**Implementation Strategy**:

### Redundancy & Failover
- **Multi-AZ Deployment**: Every component runs in 3+ availability zones
- **Database Replicas**: Automatic failover to standby within 30 seconds
- **Kafka Partition Replication**: Replicas across AZs, leader election on failure
- **Load Balancer Health Checks**: Remove failed instances within 10 seconds

### Circuit Breakers & Timeouts
- **Hystrix Pattern**: Open circuit after 5 consecutive failures
- **Timeout Budgets**: Fail fast (200ms max per external call)
- **Bulkhead Isolation**: Separate thread pools for different dependencies

### Graceful Degradation Strategies
```
Level 1: All systems operational
Level 2: Real-time results delayed (cache stale data)
Level 3: Vote submission only (read-only results page)
Level 4: Queue votes offline (process when system recovers)
```

### Chaos Engineering Practice
- **Monthly Chaos Days**: Randomly terminate 20% of pods, kill database replicas
- **Failure Injection**: Simulate network latency, dropped packets, CPU saturation
- **Game Days**: Simulate election day load + simultaneous failures

**Trade-offs**:
- Over-provisioning increases costs (3x redundancy)
- Complexity in managing degraded states
- Risk of automation making wrong decisions

**Success Metrics**:
- 99.99% uptime (52 minutes downtime/year)
- Automatic recovery from 95% of failures without human intervention
- Zero complete outages (always serve degraded service)

---

# 5. 🏗️ Overall Diagrams

## 5.1 🗂️ Overall architecture
## 5.2 🗂️ Deployment
## 5.3 🗂️ Use Cases


1. Users send requests through a global CDN + security edge
2. Traffic is validated, filtered, rate-limited, and inspected
3. Authenticated users submit votes via secure API
4. Votes are processed asynchronously
5. Data is stored redundantly and immutably
6. Real-time updates are published via streaming

---

# 6. Security & Anti-Bot Strategy (Primary Focus)

## 6.1. End-to-End Mobile Flow (React Native + Expo)

### 6.1.1 Mobile Application Stack

- Mobile framework: **React Native + Expo**
- Authentication: **Auth0**
- Liveness & Identity Verification: **SumSub**
- Bot & Device Intelligence: **FingerprintJS**
- Human Verification: **Cloudflare Turnstile**
- API Security: **JWT + OAuth2 Tokens**

This stack is designed to ensure:

- Real users only
- One-person-one-account enforcement
- Strong resistance against bots, emulators, and automation
- Secure session handling across all API calls

---

### 6.1.2 Liveness Detection & Identity Verification with SumSub

SumSub is used for:

- Facial biometrics
- Liveness detection
- Government document verification
- Global fraud risk scoring

Chosen for:

- High antifraud robustness
- Strong global compliance (KYC/AML)
- Support for multiple countries
- High-quality liveness detection against deepfake, photos, and
    replays

SumSub React Native SDK integration:

Documentation: <https://docs.sumsub.com/docs/react-native-module>

### Mobile Flow with SumSub

1. User installs and opens the React Native app.
2. During first access or registration:
    - User is asked to capture:
        - A selfie video (liveness)
        - A government-issued document
3. The app sends media directly to SumSub SDK.
4. SumSub performs:
    - Face matching
    - Liveness challenge
    - Document authenticity validation
5. The backend receives:
    - Verification status
    - Risk score
    - Unique document hash
6. Only verified users are allowed to vote.

No raw biometric data is stored directly in the voting backend.

---

### 6.1.3 Secure Authentication with Auth0 (SSO + MFA)

Auth0 is used for:

- Secure login
- Social SSO
- Passwordless login
- Multi-Factor Authentication (MFA)
- Token lifecycle management

Documentation: <https://auth0.com/docs/quickstart/native/react-native>

### Authentication Flow

1. User taps "Login".
2. React Native app redirects to Auth0 universal login.
3. Auth0 performs:
    - Credential validation
    - Social login (if enabled)
    - MFA (Authenticator App, SMS, Push, etc.)
4. On success, the app receives:
    - Access Token (short-lived)
    - ID Token
    - Refresh Token (secure storage only)

---

### 6.1.4 Bot Detection with Auth0 Challenge + Turnstile

To prevent credential stuffing, brute-force, and automated accounts:

- Auth0 Challenges are applied during:
  - Login
  - Registration
  - Password reset
- Cloudflare Turnstile is used as:
  - Invisible human challenge
  - CAPTCHA replacement
  - Bot traffic filter for mobile and web

The Turnstile token is attached to authentication requests and validated
by the backend before granting access.

---

### 6.1.5 Secure API Requests with Tokens

All API requests use:

- OAuth2 access tokens (Bearer)
- Short TTL (e.g., 15 minutes)
- Secure refresh flow
- Token binding to:
  - Device fingerprint
  - Session
  - Risk score

Example:

``` http
POST /vote
Authorization: Bearer <access_token>
```

All backend services:

- Validate the token signature
- Validate expiration and issuer
- Check device consistency
- Enforce authorization scope

---

### 6.1.6 Device Fingerprinting with FingerprintJS

FingerprintJS is used to:

- Collect passive device signals:
  - OS
  - Browser/Runtime
  - Hardware entropy
  - Emulator detection
- Generate a stable device ID
- Detect:
  - Multi-account abuse
  - Bot emulators
  - Device cloning
  - Session hijacking

How it is used:

1. FingerprintJS runs in the app runtime.
2. A device ID is generated.
3. The device ID is attached to:
    - Login requests
    - Voting requests
4. The backend correlates:
    - User Account
    - Document Hash
    - Face Template
    - Device ID

This allows detection of:

- One user trying to vote from multiple devices
- One device trying to impersonate multiple users

---

## 6.2. Architecture Overview (Edge to API)

### 6.2.1 Global Request Flow

``` text
Mobile App (React Native)
        |
        | HTTPS
        v
CloudFront + Global Edge
        |
        v
AWS WAF (Bot Control + Rate Limits)
        |
        v
AWS Global Accelerator Backbone
        |
        v
API Gateway (Rate Limited)
        |
        v
Microservices (Auth, Voting, Fraud)
```

---

### 6.2.2 CloudFront + AWS WAF Responsibilities

### CloudFront

- Global Anycast Edge
- TLS Termination
- Static caching
- Initial traffic absorption for 300M users

### AWS WAF

- IP-based rate limits
- Token-based rate limits
- Header-based rate limits
- Protection against:
  - SQL Injection
  - XSS
  - CSRF
  - API Abuse
- Integrated Bot Control

---

### 6.2.3 Global Accelerator & Backbone Routing

All traffic between edge and API uses:

- AWS Global Accelerator
- Optimized global routing
- Low-latency backbone
- Automatic regional failover

---

### 6.2.4 API Gateway Security Model

The API Gateway enforces:

- Rate limits per:
  - API Key
  - User Token
  - Device ID
- Burst protection
- Token verification
- Request signing enforcement
- Request schema validation

---

## 6.3. Tradeoffs Analysis of All Security Tools

### 6.3.1 Auth0

Pros: - Enterprise-grade authentication - Built-in MFA - Secure token
lifecycle - SSO support - High availability

Cons: - Expensive at large scale - Vendor lock-in - Limited
flexibility for custom flows

---

### 6.3.2 SumSub

Pros: - Strong biometric antifraud - Global KYC compliance -
High-quality liveness detection - Advanced risk scoring

Cons: - High user friction - Sensitive biometric data handling - High per-verification cost - Not always legally permitted for voting

---

### 6.3.3 Cloudflare Turnstile

Pros: - Invisible challenge - Better UX than CAPTCHA - Strong privacy
guarantees - Blocks simple automation

Cons: - Not sufficient alone against advanced bots - External
dependency - Needs backend verification

---

### 6.3.4 FingerprintJS

Pros: - Passive and invisible - Emulator and device cloning
detection - Excellent multi-account detection signal

Cons: - Fingerprints can be spoofed by advanced attackers - Privacy
and compliance concerns - Device replacement causes identity changes

---

### 6.3.5 AWS CloudFront

Pros: - Global CDN - Massive traffic absorption - Native integration
with AWS security - Edge-level DDoS protection

Cons: - Pricing complexity - Cache invalidation cost - Less flexible
than software-based proxies

---

### 6.3.6 AWS WAF

Pros: - Managed OWASP rules - Tight AWS integration - Native
CloudFront support - Bot Control included

 Cons: - Limited advanced behavioral fraud detection - Requires tuning
to avoid false positives

---

### 6.3.7 AWS Global Accelerator

Pros: - Very low global latency - Consistent static IPs -
Multi-region failover

Cons: - Additional cost - More complex routing model

---

### 6.3.8 API Gateway

 Pros: - Built-in rate limiting - Strong security posture - Native JWT
validation

 Cons: - Cost at very high RPS - Harder to debug than direct ALB
setups

---

# 7. Data Integrity & One-Vote Enforcement

- Globally unique voting token
- Single-use cryptographic vote key

Database enforces: - Strong uniqueness constraints - Atomic conditional
writes - Conflict detection

---

# 8. Resilience & Fault Tolerance

- Multi-AZ write replication
- Event queues for vote ingestion
- Retry with backoff
- Dead-letter queues
- Immutable audit log streams

---

# 9. Real-Time Result Distribution

- Real-time aggregation pipelines
- WebSocket / streaming consumers
- Live dashboards

---

# 10. Core Services Overview

This document describes the three core domain services of the secure
voting platform:

- **Auth Service**
- **Vote Service**
- **Fraud Service**

These services work together with external providers: - **Auth0**
(authentication) - **Sumsub** (biometric identity & liveness) - **Edge
Security / WAF** (Cloudflare or equivalent)

The goal is to ensure: - Strong identity verification - One person = one
vote - Resilience against bots and organized fraud - Full auditability

---

## 10.1 Auth Service

### 10.1.1 Purpose

The Auth Service is the **internal identity authority** of the voting
platform.\
It does NOT replace Auth0 or Sumsub. Instead, it **connects them to the
voting domain** and applies business rules.

It answers one main question: \> "Who is this user inside the voting
system, and what is their current status?"

### 10.1.2 Core Responsibilities

- Map external identities to internal voters:
  - Auth0 `sub` → `voterId`
  - Sumsub `applicantId` → `voterId`
- Maintain internal voter profile and status:
  - `PENDING_KYC`
  - `APPROVED`
  - `BLOCKED`
  - `REJECTED`
- Orchestrate onboarding and verification with Sumsub
- Enforce internal blocks coming from Fraud Service or Admin actions
- Expose voter eligibility to the Vote Service
- Maintain audit trail of identity state changes

---

### 10.1.3 Key Endpoints

#### `GET /me`

Returns the authenticated user's internal voter profile.

**Used by:** Mobile App, Web App

**Output Example:**

``` json
{
  "voterId": "vtr_123",
  "status": "APPROVED",
  "sumsubApplicantId": "applicant_987",
  "flags": {
    "isBlocked": false,
    "needsReverification": false
  }
}
```

---

#### `POST /voters/onboard`

Starts the biometric verification flow with Sumsub.

**Used by:** Mobile App after login

**What it does:** - Creates or loads voter record - Creates Sumsub
applicant - Generates Sumsub session token

**Output Example:**

``` json
{
  "voterId": "vtr_123",
  "sumsub": {
    "applicantId": "applicant_987",
    "accessToken": "sumsub-access-token",
    "flow": "document+liveness"
  },
  "status": "PENDING_KYC"
}
```

---

#### `POST /voters/webhook/sumsub`

Receives verification result from Sumsub.

**Used by:** Sumsub (Webhook)

**What it does:** - Updates voter verification status - Emits event to
Fraud & Vote systems if needed

---

#### `GET /voters/{voterId}/eligibility?electionId=...`

Checks if the voter can participate in a specific election.

**Used by:** Vote Service

**Output Example:**

``` json
{
  "voterId": "vtr_123",
  "electionId": "election_2026",
  "eligible": true
}
```

---

#### `POST /voters/{voterId}/block`

Blocks a voter at the identity level.

**Used by:** Fraud Service, Admin Panel

---

### 10.2 Vote Service

### 10.2.1 Purpose

The Vote Service is the **core election engine**.\
It is the only service allowed to **create, validate, store, and tally
votes**.

It answers the question: \> "Is this voter eligible right now, and can
we safely record this vote?"

---

### 10.2.2 Core Responsibilities

- Election creation and configuration
- Ballot management
- Eligibility validation through Auth Service
- Fraud validation through Fraud Service
- Enforcing voting rules:
  - One vote per voter
  - Idempotent submissions
- Secure vote storage (append-only / immutable)
- Vote tallying and result publishing
- Vote audit trail

---

### 10.2.3 Key Endpoints

#### `POST /elections`

Creates a new election.

---

#### `POST /votes`

Registers a vote.

**Used by:** Mobile App

**Internal Flow:** 1. Validate voter via Auth Service 2. Validate risk
via Fraud Service 3. Enforce "one person = one vote" 4. Persist vote in
immutable storage

---

#### `GET /elections/{id}/results`

Returns aggregated voting results.

---

## 10.3 Fraud Service

### 10.3.1 Purpose

The Fraud Service is the **behavioral risk engine** of the system.

It does NOT validate identity documents and does NOT authenticate
users.\
Its job is to **detect suspicious patterns across users, devices,
sessions, and elections.**

It answers the question: \> "Does this action look normal or coordinated
/ fraudulent?"

---

### 10.3.2 Core Responsibilities

- Behavioral risk scoring for:
  - Account creation
  - Voting attempts
- Device correlation:
  - Same device used by many voters
- Election-level fraud detection:
  - Coordinated vote attempts
  - Abnormal regional concentration
- Decision engine:
  - `ALLOW`
  - `CHALLENGE`
  - `DENY`
- Maintain fraud watchlists
- Maintain historical fraud signals and risk profiles
- Feed audit, alerting, and investigation tools

---

### 10.3.3 Key Endpoints

#### `POST /fraud/check-signup`

Risk analysis at voter onboarding.

**Used by:** Auth Service

**Output Example:**

``` json
{
  "decision": "ALLOW",
  "riskScore": 0.14
}
```

---

#### `POST /fraud/check-vote`

Risk analysis before vote registration.

**Used by:** Vote Service

**Output Example:**

``` json
{
  "decision": "DENY",
  "riskScore": 0.92,
  "reasons": [
    "MULTIPLE_VOTERS_SAME_DEVICE_IN_SHORT_WINDOW"
  ]
}
```

---

#### `POST /fraud/events`

Ingests behavioral events (login, vote, block, review).

---

#### `GET /fraud/profile/{voterId}`

Returns the full fraud risk profile for a voter.

---

## 10.4 Interaction Summary

  Service         Talks To        Purpose
  --------------- --------------- ---------------------------------------
  Auth Service    Auth0           Login & token validation
  Auth Service    Sumsub          Identity & biometric verification
  Auth Service    Fraud Service   Signup risk analysis
  Vote Service    Auth Service    Eligibility validation
  Vote Service    Fraud Service   Vote risk analysis
  Fraud Service   All             Receives behavioral events
  Admin Panel     All             Oversight, investigation, enforcement

---
