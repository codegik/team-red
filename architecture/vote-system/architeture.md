# 🧬 Architecture overview

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

------------------------------------------------------------------------

# 2. 🎯 Goals

### Goals

- Planet-scale availability
- Zero data loss tolerance
- High resistance to automation & fraud
- Real-time vote processing
- Fully distributed architecture

### 2.1 Never Lose Data

Voting systems are mission-critical. Any data loss leads to: - Legal
risks - Loss of public trust - Invalid election outcomes

This requires: - Multi-region replication - Strong durability
guarantees - Strict write acknowledgements - Immutable audit logs

### 2.2 Be Secure and Prevent Bots & Bad Actors (Primary Ownership Area)

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

### 2.3 Handle 300M Users

This implies: - Massive horizontal scalability - Stateless
architectures - Global CDNs - Partitioned databases - Multi-region
deployment

### 2.4 Handle 240K RPS Peak Traffic

This eliminates: - Vertical scaling - Centralized bottlenecks - Stateful
monoliths

It requires: - Load-based autoscaling - Event-driven processing -
Front-door traffic absorption - Backpressure handling

### 2.5 One Vote per User (Strict Idempotency)

This is a data + security + consistency problem: - Each identity must
be: - Verified - Unique - Non-replayable - Vote submissions must be: -
Idempotent - Conflict-safe - Race-condition proof

### 2.6 Real-Time Results

This creates challenges in: - Data streaming - Cache invalidation -
Broadcast consistency - Fan-out architectures - WebSocket / pub-sub
scalability


# 3. 🎯 Non-Goals

- On-prem or hybrid operation
- Manual moderation for fraud detection
- Single-region deployment
- Strong coupling between frontend and backend

------------------------------------------------------------------------

# 4. 📐 Principles

- Security First
- Scalability by Default
- Event-Driven Architecture
- Stateless Compute
- Multi-Layer Anti-Abuse Protection
- Auditable Data
- Failure as a Normal Condition

------------------------------------------------------------------------

# 5. 🏗️ Overall Diagrams

## 5.1 🗂️ Overall architecture

1. Users send requests through a global CDN + security edge
2. Traffic is validated, filtered, rate-limited, and inspected
3. Authenticated users submit votes via secure API
4. Votes are processed asynchronously
5. Data is stored redundantly and immutably
6. Real-time updates are published via streaming

------------------------------------------------------------------------

## 6. Security & Anti-Bot Strategy (Primary Focus)

# 6.1. End-to-End Mobile Flow (React Native + Expo)

## 6.1.1 Mobile Application Stack

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

------------------------------------------------------------------------

## 6.1.2 Liveness Detection & Identity Verification with SumSub

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

------------------------------------------------------------------------

## 6.1.3 Secure Authentication with Auth0 (SSO + MFA)

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

------------------------------------------------------------------------

## 6.1.4 Bot Detection with Auth0 Challenge + Turnstile

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

------------------------------------------------------------------------

## 6.1.5 Secure API Requests with Tokens

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

------------------------------------------------------------------------

## 6.1.6 Device Fingerprinting with FingerprintJS

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

------------------------------------------------------------------------

# 6.2. Architecture Overview (Edge to API)

## 6.2.1 Global Request Flow

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

------------------------------------------------------------------------

## 6.2.2 CloudFront + AWS WAF Responsibilities

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

------------------------------------------------------------------------

## 6.2.3 Global Accelerator & Backbone Routing

All traffic between edge and API uses:

- AWS Global Accelerator
- Optimized global routing
- Low-latency backbone
- Automatic regional failover

------------------------------------------------------------------------

## 6.2.4 API Gateway Security Model

The API Gateway enforces:

- Rate limits per:
  - API Key
  - User Token
  - Device ID
- Burst protection
- Token verification
- Request signing enforcement
- Request schema validation

------------------------------------------------------------------------

# 6.3. Tradeoffs Analysis of All Security Tools

## 6.3.1 Auth0

Pros: - Enterprise-grade authentication - Built-in MFA - Secure token
lifecycle - SSO support - High availability

Cons: - Expensive at large scale - Vendor lock-in - Limited
flexibility for custom flows

------------------------------------------------------------------------

## 6.3.2 SumSub

Pros: - Strong biometric antifraud - Global KYC compliance -
High-quality liveness detection - Advanced risk scoring

Cons: - High user friction - Sensitive biometric data handling - High per-verification cost - Not always legally permitted for voting

------------------------------------------------------------------------

## 6.3.3 Cloudflare Turnstile

Pros: - Invisible challenge - Better UX than CAPTCHA - Strong privacy
guarantees - Blocks simple automation

Cons: - Not sufficient alone against advanced bots - External
dependency - Needs backend verification

------------------------------------------------------------------------

## 6.3.4 FingerprintJS

Pros: - Passive and invisible - Emulator and device cloning
detection - Excellent multi-account detection signal

Cons: - Fingerprints can be spoofed by advanced attackers - Privacy
and compliance concerns - Device replacement causes identity changes

------------------------------------------------------------------------

## 6.3.5 AWS CloudFront

Pros: - Global CDN - Massive traffic absorption - Native integration
with AWS security - Edge-level DDoS protection

Cons: - Pricing complexity - Cache invalidation cost - Less flexible
than software-based proxies

------------------------------------------------------------------------

## 6.3.6 AWS WAF

Pros: - Managed OWASP rules - Tight AWS integration - Native
CloudFront support - Bot Control included

 Cons: - Limited advanced behavioral fraud detection - Requires tuning
to avoid false positives

------------------------------------------------------------------------

## 6.3.7 AWS Global Accelerator

Pros: - Very low global latency - Consistent static IPs -
Multi-region failover

Cons: - Additional cost - More complex routing model

------------------------------------------------------------------------

## 6.3.8 API Gateway

 Pros: - Built-in rate limiting - Strong security posture - Native JWT
validation

 Cons: - Cost at very high RPS - Harder to debug than direct ALB
setups

------------------------------------------------------------------------

# 7. 💾 Migrations

We don't have migration for this architecture since its a new system.

------------------------------------------------------------------------

# 8. 🧪 Testing strategy

## Frontend Tests
- ReactJS component rendering tests with focus on performance metrics.
- Client-side state management tests.
- WebSocket client implementation tests.

## Contract tests
- Test API contracts between decomposed microservices.
- Verify WebSocket message formats and protocols.

## Integration tests
- Try to cover most of the scenarios.
- Test WebSocket real-time communication flows.
- Run in isolated environments before production deployment.

## Infra tests
- Validate Global Accelerator routing behavior.

## Performance tests
- Use K6 to simulate the user behavior and check the system's performance.
- Measure database query performance under load
- Measure UI rendering time across device types
- Benchmark WebSocket vs HTTP performance in real usage scenarios
- Track CDN cache hit/miss ratios
- Execute in staging environment with production-like conditions

## Chaos tests
- Simulate AWS region failures to test Global Accelerator failover
- Test WebSocket reconnection strategies during network disruptions
- Inject latency between services to identify performance bottlenecks
- Execute in isolated production environment during low-traffic periods

## Mobile testing

- Unit Android: ViewModel/repository with JUnit.
- Unit iOS: XCTest with async/await; mocks per protocol.
- UI Android: Espresso for flows (login, search, dojo).
- UI iOS: XCUITest with LaunchArguments for mocks.
- Network/Contract: MockWebServer (Android) / URLProtocol stub (iOS); Pact consumer tests for contracts with the backend.
- Performance: Cold start and WS connection times measured in CI (staging).
- Accessibility: Basic TalkBack/VoiceOver per critical screen.

------------------------------------------------------------------------

## 7. Data Integrity & One-Vote Enforcement

- Globally unique voting token
- Single-use cryptographic vote key

Database enforces: - Strong uniqueness constraints - Atomic conditional
writes - Conflict detection

------------------------------------------------------------------------

## 8. Resilience & Fault Tolerance

- Multi-AZ write replication
- Event queues for vote ingestion
- Retry with backoff
- Dead-letter queues
- Immutable audit log streams

------------------------------------------------------------------------

## 9. Real-Time Result Distribution

- Real-time aggregation pipelines
- WebSocket / streaming consumers
- Live dashboards

### 9.1 Observability and Monitoring

A robust observability strategy is critical for a system of this scale and criticality. We adopt the **three pillars of observability**: Metrics, Logs, and Traces and all using Open Source tools.

#### 9.1.1 Observability Stack Overview

| Pillar | Tool | Purpose |
|--------|------|---------|
| Metrics | Prometheus | Time-series collection and alerting |
| Visualization | Grafana | Dashboards and unified observability UI |
| Tracing | Jaeger | Distributed tracing |
| Logs | Loki | Log aggregation (Prometheus-native) |
| Alerting | Alertmanager | Alert routing and notification |
| Service Mesh Observability | OpenTelemetry | Instrumentation standard |

---

### 9.1.2 Metrics - Prometheus

Prometheus is the core metrics engine, chosen for its Open Source nature and Kubernetes-native design.

**Key Features:**
- Pull-based time-series collection via exporters
- Powerful query language (PromQL)
- Built-in alerting rules
- Service discovery for dynamic environments
- Integration with AWS via CloudWatch Exporter

**Exporters to Deploy:**
- `node_exporter` - Host-level metrics (CPU, memory, disk, network)
- `kube-state-metrics` - Kubernetes object states
- `cloudwatch_exporter` - AWS service metrics (RDS, SQS, ElastiCache, etc.)
- `blackbox_exporter` - Endpoint probing (HTTP, TCP, DNS)
- Custom application exporters for each microservice

**Critical Metrics to Monitor:**

| Category | Metrics |
|----------|---------|
| Application | Request rate, error rate, latency (p50, p95, p99) |
| Business | Votes per second, fraud detection rate, auth success rate |
| Infrastructure | CPU, memory, disk I/O, network throughput |
| Kubernetes | Pod restarts, pending pods, node availability |
| Database | Connection pool, query latency, replication lag |
| Queue | Message age, queue depth, consumer lag |

**Prometheus Architecture for Scale:**

```
┌─────────────────────────────────────────────────────────────────┐
│                     Prometheus Federation                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │ Prometheus  │  │ Prometheus  │  │ Prometheus  │              │
│  │  Region A   │  │  Region B   │  │  Region C   │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
│         │                │                │                      │
│         └────────────────┼────────────────┘                      │
│                          │                                        │
│                          ▼                                        │
│                 ┌─────────────────┐                              │
│                 │ Global Prometheus│                              │
│                 │   (Federation)   │                              │
│                 └────────┬────────┘                              │
│                          │                                        │
│                          ▼                                        │
│                 ┌─────────────────┐                              │
│                 │    Thanos /     │ ← Long-term storage          │
│                 │   Cortex (opt)  │                              │
│                 └─────────────────┘                              │
└─────────────────────────────────────────────────────────────────┘
```

For 300M users and high cardinality, consider **Thanos** or **Cortex** for:
- Long-term metric storage (S3-backed)
- Global query view across regions
- Downsampling for historical data

---

### 9.1.3 Visualization - Grafana

Grafana serves as the unified observability frontend.

**Key Features:**
- Multi-datasource support (Prometheus, Loki, Jaeger, CloudWatch)
- Rich dashboard templating
- Alerting integration
- Team-based access control
- Annotation support for deployment markers

**Recommended Dashboards:**

1. **System Overview**
   - Global request rate and error rate
   - Vote submission success rate
   - Active users and concurrent connections
   - Regional health status

2. **Service-Level Dashboards** (per microservice)
   - RED metrics (Rate, Errors, Duration)
   - Dependency health
   - Resource utilization

3. **Security & Fraud Dashboard**
   - Bot detection triggers per minute
   - Fraud check decisions (ALLOW/CHALLENGE/DENY)
   - Auth failures and suspicious patterns
   - WAF block rate

4. **Infrastructure Dashboard**
   - Kubernetes cluster health
   - Node resource saturation
   - Database replication status
   - Queue depths and consumer lag

5. **Business Metrics Dashboard**
   - Total votes cast (real-time)
   - Votes per region/election
   - User registration funnel
   - Peak traffic patterns

---

### 9.1.4 Distributed Tracing - Jaeger

For a microservices architecture at this scale, distributed tracing is essential to understand request flow and identify bottlenecks.

**Why Jaeger:**
- Open Source (CNCF graduated project)
- Native OpenTelemetry support
- Scalable architecture with Kafka and Elasticsearch/Cassandra backends
- Adaptive sampling for high-volume systems

**Tracing Architecture:**

```
┌──────────────────────────────────────────────────────────────────────┐
│                         Tracing Flow                                  │
├──────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐           │
│  │ Mobile  │───▶│   API   │───▶│  Auth   │───▶│  Vote   │           │
│  │   App   │    │ Gateway │    │ Service │    │ Service │           │
│  └─────────┘    └────┬────┘    └────┬────┘    └────┬────┘           │
│                      │              │              │                  │
│         ┌────────────┴──────────────┴──────────────┘                 │
│         │  trace-id: abc123                                           │
│         │  span-id propagated via headers                            │
│         ▼                                                             │
│  ┌─────────────────────────────────────────────────────────┐         │
│  │              OpenTelemetry Collector                     │         │
│  │  (sampling, batching, export to Jaeger)                 │         │
│  └────────────────────────┬────────────────────────────────┘         │
│                           │                                           │
│                           ▼                                           │
│  ┌─────────────────────────────────────────────────────────┐         │
│  │                    Jaeger Backend                        │         │
│  │  ┌──────────┐    ┌─────────────┐    ┌──────────────┐   │         │
│  │  │Collector │───▶│    Kafka    │───▶│ Elasticsearch│   │         │
│  │  └──────────┘    └─────────────┘    └──────────────┘   │         │
│  └─────────────────────────────────────────────────────────┘         │
│                                                                        │
└──────────────────────────────────────────────────────────────────────┘
```

**Instrumentation Strategy:**

| Service | Instrumentation |
|---------|-----------------|
| API Gateway | Auto-instrumentation via OpenTelemetry |
| Auth Service | Manual spans for Auth0/Sumsub calls |
| Vote Service | Manual spans for eligibility check, fraud check, vote persist |
| Fraud Service | Manual spans for risk scoring pipeline |
| Database calls | Auto-instrumentation with query tagging |
| External APIs | Manual spans with timeout tracking |

**Key Spans to Capture:**

- `http.request` - Incoming HTTP requests
- `auth.validate_token` - Token validation
- `auth.check_eligibility` - Voter eligibility check
- `fraud.check_vote` - Fraud risk assessment
- `vote.persist` - Vote storage operation
- `db.query` - Database operations
- `external.auth0` - Auth0 API calls
- `external.sumsub` - Sumsub API calls

**Sampling Strategy:**

For 240K RPS, full tracing is not feasible. Use adaptive sampling:
- 100% sampling for errors and high-latency requests
- 1% sampling for successful requests
- 100% sampling for fraud-flagged requests
- Head-based sampling with tail-based upgrade for anomalies

---

### 9.1.5 Log Aggregation - Loki

Loki provides log aggregation that integrates natively with Grafana and uses the same label model as Prometheus.

**Why Loki:**
- Open Source (Grafana Labs)
- Lightweight - indexes labels only, not full text
- Cost-effective storage (S3-backed)
- Native Grafana integration
- LogQL query language (similar to PromQL)

**Log Architecture:**

```
┌──────────────────────────────────────────────────────────────┐
│                      Log Pipeline                             │
├──────────────────────────────────────────────────────────────┤
│                                                                │
│  ┌──────────────┐                                             │
│  │ Microservice │──┐                                          │
│  └──────────────┘  │                                          │
│  ┌──────────────┐  │    ┌──────────┐    ┌──────────────┐     │
│  │ Microservice │──┼───▶│ Promtail │───▶│     Loki     │     │
│  └──────────────┘  │    │ (Agent)  │    │   (Storage)  │     │
│  ┌──────────────┐  │    └──────────┘    └──────┬───────┘     │
│  │ Microservice │──┘                           │              │
│  └──────────────┘                              ▼              │
│                                          ┌──────────┐         │
│                                          │ Grafana  │         │
│                                          │  (Query) │         │
│                                          └──────────┘         │
└──────────────────────────────────────────────────────────────┘
```

**Structured Logging Standard:**

All services must emit structured JSON logs:

```json
{
  "timestamp": "2026-01-15T10:30:00Z",
  "level": "INFO",
  "service": "vote-service",
  "trace_id": "abc123",
  "span_id": "def456",
  "voter_id": "vtr_789",
  "election_id": "election_2026",
  "message": "Vote recorded successfully",
  "duration_ms": 45
}
```

**Required Log Labels:**
- `service` - Service name
- `environment` - prod/staging/dev
- `region` - AWS region
- `level` - Log level (ERROR, WARN, INFO, DEBUG)

**Log Retention Policy:**
- ERROR logs: 90 days
- WARN logs: 30 days
- INFO logs: 14 days
- DEBUG logs: 3 days (staging only)

---

### 9.1.6 Alerting - Alertmanager

Prometheus Alertmanager handles alert routing, deduplication, and notification.

**Alert Categories:**

| Severity | Response Time | Examples |
|----------|---------------|----------|
| Critical (P1) | < 5 min | Service down, data loss risk, security breach |
| High (P2) | < 30 min | High error rate, degraded performance |
| Medium (P3) | < 4 hours | Elevated latency, resource warnings |
| Low (P4) | Next business day | Non-critical warnings |

**Critical Alerts for Voting System:**

```yaml
# Example alert rules
groups:
  - name: voting-critical
    rules:
      - alert: VoteServiceDown
        expr: up{job="vote-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Vote Service is down"

      - alert: HighVoteErrorRate
        expr: rate(vote_errors_total[5m]) / rate(vote_requests_total[5m]) > 0.01
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Vote error rate exceeds 1%"

      - alert: FraudSpikeDetected
        expr: rate(fraud_denied_total[5m]) > 100
        for: 5m
        labels:
          severity: high
        annotations:
          summary: "Unusual fraud detection spike"

      - alert: DatabaseReplicationLag
        expr: mysql_slave_lag_seconds > 5
        for: 3m
        labels:
          severity: critical
        annotations:
          summary: "Database replication lag exceeds 5 seconds"
```

**Notification Channels:**
- Critical: PagerDuty + Slack + SMS
- High: Slack + Email
- Medium: Slack
- Low: Email digest

---

### 9.1.7 Instrumentation Standard - OpenTelemetry

OpenTelemetry (OTel) provides a vendor-neutral instrumentation standard across all services.

**Why OpenTelemetry:**
- CNCF standard
- Unified API for metrics, traces, and logs
- Auto-instrumentation for common frameworks
- Collector for processing and routing telemetry

**OpenTelemetry Collector Configuration:**

```
┌─────────────────────────────────────────────────────────────────┐
│                 OpenTelemetry Collector                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  Receivers          Processors           Exporters               │
│  ┌─────────┐       ┌───────────┐       ┌────────────┐           │
│  │  OTLP   │──────▶│  Batch    │──────▶│ Prometheus │           │
│  │  gRPC   │       │  Sampling │       │   (metrics)│           │
│  └─────────┘       │  Filter   │       ├────────────┤           │
│  ┌─────────┐       └───────────┘       │   Jaeger   │           │
│  │  OTLP   │────────────────────────▶  │  (traces)  │           │
│  │  HTTP   │                           ├────────────┤           │
│  └─────────┘                           │    Loki    │           │
│                                        │   (logs)   │           │
│                                        └────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

**SDK Integration per Language:**
- Node.js: `@opentelemetry/sdk-node`
- Go: `go.opentelemetry.io/otel`
- Python: `opentelemetry-sdk`
- Java: `opentelemetry-java`

---

### 9.1.8 Correlation and Context Propagation

For effective debugging, all telemetry must be correlated:

**Correlation IDs:**
- `trace_id` - Unique per request, propagated across services
- `span_id` - Unique per operation
- `voter_id` - Business context
- `election_id` - Business context
- `request_id` - API Gateway assigned

**Context Propagation:**
- HTTP: W3C Trace Context headers (`traceparent`, `tracestate`)
- Kafka: Headers with trace context
- gRPC: Metadata with trace context

**Grafana Correlation:**
- Click on metric → Jump to related traces
- Click on trace → Jump to related logs
- Unified view with `trace_id` as the correlation key

---

### 9.1.9 Observability for Specific Components

**WebSocket Connections (Real-time Results):**
- Active connection count per region
- Message throughput (in/out)
- Connection duration histogram
- Reconnection rate

**Fraud Service:**
- Risk score distribution
- Decision latency (p99 < 100ms)
- False positive rate (requires manual labeling)
- Model inference time

**External Dependencies:**
- Auth0 API latency and error rate
- Sumsub API latency and error rate
- AWS service health (via CloudWatch)

---

### 9.1.10 Observability Infrastructure Sizing

| Component | Sizing Recommendation |
|-----------|----------------------|
| Prometheus | 3 replicas per region, 500GB storage each |
| Thanos | Sidecar per Prometheus, centralized Query + Store |
| Jaeger Collector | 3 replicas, auto-scaling on CPU |
| Jaeger Storage | Elasticsearch 3-node cluster, 1TB+ |
| Loki | 3 ingesters, 3 queriers, S3 storage |
| Grafana | 2 replicas behind LB, PostgreSQL backend |
| OTel Collector | DaemonSet on all nodes |

---

### 9.1.11 Runbooks and SLOs

**Service Level Objectives:**

| Service | SLO | Error Budget |
|---------|-----|--------------|
| Vote Submission | 99.99% success rate | 0.01% (~26 min/month) |
| Vote Latency | p99 < 500ms | - |
| Auth Service | 99.95% availability | 0.05% |
| Real-time Results | 99.9% availability | 0.1% |
| Fraud Check Latency | p99 < 200ms | - |

**Runbooks to Create:**
- Vote Service degradation
- Database failover
- Fraud spike response
- DDoS attack response
- Regional failover procedure 


------------------------------------------------------------------------

## Core Services Overview

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

------------------------------------------------------------------------

## 1. Auth Service

### Purpose

The Auth Service is the **internal identity authority** of the voting
platform.\
It does NOT replace Auth0 or Sumsub. Instead, it **connects them to the
voting domain** and applies business rules.

It answers one main question: \> "Who is this user inside the voting
system, and what is their current status?"

### Core Responsibilities

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

------------------------------------------------------------------------

### Key Endpoints

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

------------------------------------------------------------------------

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

------------------------------------------------------------------------

#### `POST /voters/webhook/sumsub`

Receives verification result from Sumsub.

**Used by:** Sumsub (Webhook)

**What it does:** - Updates voter verification status - Emits event to
Fraud & Vote systems if needed

------------------------------------------------------------------------

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

------------------------------------------------------------------------

#### `POST /voters/{voterId}/block`

Blocks a voter at the identity level.

**Used by:** Fraud Service, Admin Panel

------------------------------------------------------------------------

## 2. Vote Service

### Purpose

The Vote Service is the **core election engine**.\
It is the only service allowed to **create, validate, store, and tally
votes**.

It answers the question: \> "Is this voter eligible right now, and can
we safely record this vote?"

------------------------------------------------------------------------

### Core Responsibilities

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

------------------------------------------------------------------------

### Key Endpoints

#### `POST /elections`

Creates a new election.

------------------------------------------------------------------------

#### `POST /votes`

Registers a vote.

**Used by:** Mobile App

**Internal Flow:** 1. Validate voter via Auth Service 2. Validate risk
via Fraud Service 3. Enforce "one person = one vote" 4. Persist vote in
immutable storage

------------------------------------------------------------------------

#### `GET /elections/{id}/results`

Returns aggregated voting results.

------------------------------------------------------------------------

## 3. Fraud Service

### Purpose

The Fraud Service is the **behavioral risk engine** of the system.

It does NOT validate identity documents and does NOT authenticate
users.\
Its job is to **detect suspicious patterns across users, devices,
sessions, and elections.**

It answers the question: \> "Does this action look normal or coordinated
/ fraudulent?"

------------------------------------------------------------------------

### Core Responsibilities

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

------------------------------------------------------------------------

### Key Endpoints

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

------------------------------------------------------------------------

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

------------------------------------------------------------------------

#### `POST /fraud/events`

Ingests behavioral events (login, vote, block, review).

------------------------------------------------------------------------

#### `GET /fraud/profile/{voterId}`

Returns the full fraud risk profile for a voter.

------------------------------------------------------------------------

## Interaction Summary

  Service         Talks To        Purpose
  --------------- --------------- ---------------------------------------
  Auth Service    Auth0           Login & token validation
  Auth Service    Sumsub          Identity & biometric verification
  Auth Service    Fraud Service   Signup risk analysis
  Vote Service    Auth Service    Eligibility validation
  Vote Service    Fraud Service   Vote risk analysis
  Fraud Service   All             Receives behavioral events
  Admin Panel     All             Oversight, investigation, enforcement

------------------------------------------------------------------------
