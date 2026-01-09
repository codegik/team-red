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

## 6.3.1 SumSub

Pros: - Strong biometric antifraud - Global KYC compliance -
High-quality liveness detection - Advanced risk scoring

Cons: - High user friction - Sensitive biometric data handling - High per-verification cost - Not always legally permitted for voting

------------------------------------------------------------------------

## 6.3.2 Cloudflare Turnstile

Pros: - Invisible challenge - Better UX than CAPTCHA - Strong privacy
guarantees - Blocks simple automation

Cons: - Not sufficient alone against advanced bots - External
dependency - Needs backend verification

------------------------------------------------------------------------

## 6.3.3 FingerprintJS

Pros: - Passive and invisible - Emulator and device cloning
detection - Excellent multi-account detection signal

Cons: - Fingerprints can be spoofed by advanced attackers - Privacy
and compliance concerns - Device replacement causes identity changes

------------------------------------------------------------------------

## 6.3.4 AWS CloudFront

Pros: - Global CDN - Massive traffic absorption - Native integration
with AWS security - Edge-level DDoS protection

Cons: - Pricing complexity - Cache invalidation cost - Less flexible
than software-based proxies

------------------------------------------------------------------------

## 6.3.5 AWS WAF

Pros: - Managed OWASP rules - Tight AWS integration - Native
CloudFront support - Bot Control included

 Cons: - Limited advanced behavioral fraud detection - Requires tuning
to avoid false positives

------------------------------------------------------------------------

## 6.3.6 AWS Global Accelerator

Pros: - Very low global latency - Consistent static IPs -
Multi-region failover

Cons: - Additional cost - More complex routing model

------------------------------------------------------------------------

## 6.3.7 API Gateway

 Pros: - Built-in rate limiting - Strong security posture - Native JWT
validation

 Cons: - Cost at very high RPS - Harder to debug than direct ALB
setups

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
