# Realtime Voting System -- Architecture Overview

## 1. Context & Problem Statement

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

### Restricted Technologies (Non-Allowed)

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

## 2. Core Requirements & Why They Matter

### 2.1 Never Lose Data

Voting systems are mission-critical. Any data loss leads to: - Legal
risks - Loss of public trust - Invalid election outcomes

This requires: - Multi-region replication - Strong durability
guarantees - Strict write acknowledgements - Immutable audit logs

------------------------------------------------------------------------

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

------------------------------------------------------------------------

### 2.3 Handle 300M Users

This implies: - Massive horizontal scalability - Stateless
architectures - Global CDNs - Partitioned databases - Multi-region
deployment

------------------------------------------------------------------------

### 2.4 Handle 240K RPS Peak Traffic

This eliminates: - Vertical scaling - Centralized bottlenecks - Stateful
monoliths

It requires: - Load-based autoscaling - Event-driven processing -
Front-door traffic absorption - Backpressure handling

------------------------------------------------------------------------

### 2.5 One Vote per User (Strict Idempotency)

This is a data + security + consistency problem: - Each identity must
be: - Verified - Unique - Non-replayable - Vote submissions must be: -
Idempotent - Conflict-safe - Race-condition proof

------------------------------------------------------------------------

### 2.6 Real-Time Results

This creates challenges in: - Data streaming - Cache invalidation -
Broadcast consistency - Fan-out architectures - WebSocket / pub-sub
scalability

------------------------------------------------------------------------

## 3. Goals & Non-Goals

### Goals

- Planet-scale availability
- Zero data loss tolerance
- High resistance to automation & fraud
- Real-time vote processing
- Fully distributed architecture

### Non-Goals

- On-prem or hybrid operation
- Manual moderation for fraud detection
- Single-region deployment
- Strong coupling between frontend and backend

------------------------------------------------------------------------

## 4. Design Principles

- Security First
- Scalability by Default
- Event-Driven Architecture
- Stateless Compute
- Multi-Layer Anti-Abuse Protection
- Auditable Data
- Failure as a Normal Condition

------------------------------------------------------------------------

## 5. High-Level Architecture Overview

1. Users send requests through a global CDN + security edge
2. Traffic is validated, filtered, rate-limited, and inspected
3. Authenticated users submit votes via secure API
4. Votes are processed asynchronously
5. Data is stored redundantly and immutably
6. Real-time updates are published via streaming

------------------------------------------------------------------------

## 5.2. Security & Anti-Bot Strategy (Primary Focus)

## 5.2.1 Mobile Application Stack

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

## 5.2.2 Liveness Detection & Identity Verification with SumSub

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

## 5.2.3 Secure Authentication with Auth0 (SSO + MFA)

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

## 5.2.4 Bot Detection with Auth0 Challenge + Turnstile

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

## 5.2.5 Secure API Requests with Tokens

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

## 5.2.6 Device Fingerprinting with FingerprintJS

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

# 5.3. Architecture Overview (Edge to API)

## 5.3.1 Global Request Flow

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

## 5.3.2 CloudFront + AWS WAF Responsibilities

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

## 5.3.3 Global Accelerator & Backbone Routing

All traffic between edge and API uses:

- AWS Global Accelerator
- Optimized global routing
- Low-latency backbone
- Automatic regional failover

------------------------------------------------------------------------

## 5.3.4 API Gateway Security Model

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

# 5.4. Tradeoffs Analysis of All Security Tools

## 5.4.1 SumSub

Pros: - Strong biometric antifraud - Global KYC compliance -
High-quality liveness detection - Advanced risk scoring

Cons: - High user friction - Sensitive biometric data handling - High per-verification cost - Not always legally permitted for voting

------------------------------------------------------------------------

## 5.4.2 Cloudflare Turnstile

Pros: - Invisible challenge - Better UX than CAPTCHA - Strong privacy
guarantees - Blocks simple automation

Cons: - Not sufficient alone against advanced bots - External
dependency - Needs backend verification

------------------------------------------------------------------------

## 5.4.3 FingerprintJS

Pros: - Passive and invisible - Emulator and device cloning
detection - Excellent multi-account detection signal

Cons: - Fingerprints can be spoofed by advanced attackers - Privacy
and compliance concerns - Device replacement causes identity changes

------------------------------------------------------------------------

## 5.4.4 AWS CloudFront

Pros: - Global CDN - Massive traffic absorption - Native integration
with AWS security - Edge-level DDoS protection

Cons: - Pricing complexity - Cache invalidation cost - Less flexible
than software-based proxies

------------------------------------------------------------------------

## 5.4.5 AWS WAF

Pros: - Managed OWASP rules - Tight AWS integration - Native
CloudFront support - Bot Control included

 Cons: - Limited advanced behavioral fraud detection - Requires tuning
to avoid false positives

------------------------------------------------------------------------

## 5.4.6 AWS Global Accelerator

Pros: - Very low global latency - Consistent static IPs -
Multi-region failover

Cons: - Additional cost - More complex routing model

------------------------------------------------------------------------

## 5.4.7 API Gateway

 Pros: - Built-in rate limiting - Strong security posture - Native JWT
validation

 Cons: - Cost at very high RPS - Harder to debug than direct ALB
setups

------------------------------------------------------------------------

# 6. 💾 Migrations

We don't have migration for this architecture since its a new system.

------------------------------------------------------------------------

# 7. 🧪 Testing strategy

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

## 8. Data Integrity & One-Vote Enforcement
>>>>>>>
>>>>>>> Stashed changes

- Globally unique voting token
- Single-use cryptographic vote key

Database enforces: - Strong uniqueness constraints - Atomic conditional
writes - Conflict detection

------------------------------------------------------------------------

## 9. Resilience & Fault Tolerance

- Multi-AZ write replication
- Event queues for vote ingestion
- Retry with backoff
- Dead-letter queues
- Immutable audit log streams

------------------------------------------------------------------------

## 10. Real-Time Result Distribution

- Real-time aggregation pipelines
- WebSocket / streaming consumers
- Live dashboards

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

## 2. Notification Service

## 3. Pub Service

## 4. Sub Service
