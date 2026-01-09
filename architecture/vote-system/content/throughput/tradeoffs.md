# Messaging Trade-offs for a Large-Scale Voting System

## Context

This document compares three messaging/streaming approaches for a large-scale voting system and provides a recommendation.

**Target scale:**

* ~250,000 requests per second (RPS)
* ~300 million registered users

**Core requirements:**

* Never lose votes
* Ensure users can vote only once
* Absorb extreme traffic spikes
* Operate reliably under attack or partial failures
* Keep operational complexity manageable

The three options evaluated are:

1. SNS + SQS (AWS managed messaging)
2. Amazon MSK (Managed Kafka)
3. Self-managed Kafka

---

## 1. SNS + SQS

### Strengths

* Fully managed, low operational overhead
* Native backpressure handling via queues
* Simple retry and Dead Letter Queue (DLQ) support
* Scales horizontally with predictable behavior
* Very strong fit for task-based, critical workflows

### Weaknesses

* Not designed as an event log
* Limited replay capabilities without additional storage
* Ordering guarantees only available with SQS FIFO (with throughput limits)

### Operational Characteristics

* Excellent fault isolation
* Simple autoscaling model
* Easy integration with AWS IAM, Lambda, ECS, EKS

### Best Fit

* High-throughput ingestion pipelines
* Mission-critical workflows where durability and retries matter more than replay

---

## 2. Amazon MSK (Managed Kafka)

### Strengths

* True event streaming and durable event log
* Native support for multiple independent consumers
* Replay and retention are first-class features
* Very high throughput when properly partitioned

### Weaknesses

* Kafka complexity still applies (partitioning, rebalancing, tuning)
* Retry and DLQ patterns must be implemented manually
* Higher operational and cognitive load than SNS/SQS

### Operational Characteristics

* Requires careful capacity planning
* Monitoring lag and rebalancing is critical
* More engineering effort for failure handling

### Best Fit

* Analytics pipelines
* Event-driven architectures with replay and reprocessing needs
* Real-time monitoring, antifraud, and BI streams

---

## 3. Self-Managed Kafka

### Strengths

* Full control over configuration and topology
* Potentially lower cost at very large scale
* Suitable for multi-cloud or on-premise strategies

### Weaknesses

* Highest operational complexity
* Requires dedicated SRE / platform team
* Higher risk of data loss or outages if mismanaged

### Operational Characteristics

* Manual upgrades, scaling, and incident response
* High engineering cost

### Best Fit

* Organizations with deep Kafka expertise
* Strict compliance or infrastructure control requirements

---

## Comparative Summary

| Aspect                 | SNS + SQS | Amazon MSK | Self-Managed Kafka |
| ---------------------- | --------- | ---------- | ------------------ |
| Operational Complexity | Low       | Medium     | High               |
| Backpressure Handling  | Native    | Manual     | Manual             |
| Replay Capability      | Weak      | Strong     | Strong             |
| Retry / DLQ            | Native    | Custom     | Custom             |
| Scalability            | Excellent | Excellent  | Excellent          |
| Cost Predictability    | High      | Medium     | Low                |
