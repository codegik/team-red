# Architecture Tradeoffs

This document captures the key architectural decisions and their tradeoffs for the Vote System.

## Decision Categories

| Category                  | Decision              | Chosen               | Rejected                          | Rationale                                                        |
| ------------------------- | --------------------- | -------------------- | --------------------------------- | ---------------------------------------------------------------- |
| **Database**              | Data store            | PostgreSQL (RDS)     | MongoDB, NoSQL                    | Guarantees required for vote integrity                           |
| **Database**              | Scaling strategy      | Geo-based sharding   | Single instance, vertical scaling | 300M users require horizontal scaling                            |
| **Database**              | Audit/Logs store      | OpenSearch.          | PostgreSQL, DynamoDB              | Add content                                                      |
| **Cloud Provider**        | Infrastructure        | AWS                  | GCP, Azure, On-premise            | Add content                                                      |
| **Architecture**          | Style                 | Microservices        | Monolith                          | Scale requirements; independent service scaling                  |
| **Authentication**        | Provider              | Auth0                | Custom, Cognito                   | MFA, SSO;                                                        |
| **Identity Verification** | Provider              | SumSub               | Jumio, Onfido                     | Add content                                                      |
| **Bot Detection**         | Human verification    | Cloudflare Turnstile | reCAPTCHA, hCaptcha               | Add content                                                      |
| **Bot Detection**         | Device fingerprinting | FingerprintJS        | Custom solution                   | Add content                                                      |
| **Compute**               | Runtime               | Kubernetes (EKS)     | ECS, Lambda                       | Add content                                                      |
| **Messaging**             | Event streaming       | Kafka                | SQS, RabbitMQ                     | High throughput for 250k RPSAdd content                          |
| **Caching**               | Layer                 | Redis                | Memcached, ElastiCache            | Real-time counters, session data, rate limiting                  |
| **Real-time**             | Updates               | WebSocket + SSE      | Polling, Long-polling             | True real-time results; accepts connection management complexity |

## Security Tradeoffs Summary

| Tool              | Pros                                        | Cons               | Risk Accepted     |
| ----------------- | ------------------------------------------- | ------------------ | ----------------- |
| **Auth0**         | Enterprise MFA, secure token lifecycle, SSO | Expensive at scale | Cost for security |
| **SumSub**        | Strong biometric antifraud, global KYC      | add cons           | add risk          |
| **Turnstile**     | Good UX                                     | add cons           | add risk          |
| **FingerprintJS** | Passive, emulator detection                 | add cons           | add risk          |
| **AWS WAF**       | Managed rules, native integration           | add cons           | add risk          |

## Scalability Tradeoffs

| Requirement       | Solution                                 | Tradeoff |
| ----------------- | ---------------------------------------- | -------- |
| 300M users        | Geo-based DB sharding                    | Add      |
| 250k RPS          | HPA + KEDA autoscaling                   | Add      |
| Zero data loss    | Synchronous replication, WAL             | Add      |
| One-vote-per-user | DB unique constraints + idempotency keys | Add      |
| Real-time results | Event streaming + WebSocket fan-out      | Add      |

## Key Principles Applied

1. **Security over convenience**
2. **Consistency over availability**
3. **Managed services over custom**
4. **Horizontal over vertical scaling**
