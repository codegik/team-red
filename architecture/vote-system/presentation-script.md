# Presentation Script — Realtime Voting System Architecture

---

## 1 - Introduction — sections 1.1 to 1.3, 2, 3

> *"Hello everyone. Today we are going to present the architecture of our Real-Time Voting System.*
>
> *The system needs to support up to 300 million users and handle up to 240,000 requests per second at peak times. That is a lot of traffic.*
>
> *The key requirements are: security, one vote per person, no data loss, and real-time results. Everything we designed was based on these four goals."*

---

## 2 - Architecture Diagram — sections 5.1, 5.2

> *"Now, let's look at the overall architecture diagram.*
>
> *The first thing a user does is open the app — mobile or web. The request travels through **CloudFront**, our global CDN. This is important because users can be anywhere in the world.*
>
> *Before any request reaches our servers, it goes through the **AWS WAF**. WAF means Web Application Firewall. We added WAF because one of our requirements is security — it blocks bots, SQL injection, and bad traffic. Without it, we have no protection at the edge.*
>
> *Then, the request goes through the **AWS Global Accelerator**, which routes the traffic to the closest AWS region. This helps us meet our latency goals.*
>
> *After that, the request reaches the **API Gateway**, which applies rate limiting. This is our second layer of protection — even if something passes the WAF, the API Gateway controls how many requests a single user can send.*
>
> *Finally, the request reaches our **microservices**. Each service is independent, which means we can scale them separately. This is critical to support 240,000 requests per second without bringing everything down at once."*

---

## 3 - Solution Diagram — sections 5.3, 13.1 to 13.4

> *"Now let's talk about the solution diagram — how our services work together.*
>
> *We have four main services:*
>
> ***Auth Service (13.1)** — This service manages login and identity. A user authenticates with Google, Facebook, or Apple. We also use **SumSub** for identity verification — the user takes a selfie and sends a government ID. This is critical for the one-vote-per-person requirement. We cannot let someone create two accounts.*
>
> ***Vote Service (13.2)** — This is the core of the system. When a user submits a vote, the service first checks if the user already voted, using **Redis** as a fast cache. If it is a new vote, it publishes an event to **Kafka**. We chose Kafka because it gives us exactly-once delivery — this is very important: one event, one vote, no duplicates.*
>
> *The Vote Service also consumes from Kafka, persists the vote to **PostgreSQL**, and then updates the vote counter in Redis.*
>
> ***Notification Service (13.3)** — After a vote is counted, the Notification Service reads that event from Kafka and sends the updated results to all connected users via **WebSockets**. We chose WebSockets because it is a persistent connection with lower latency — users see results updating in real time, not after a page refresh.*
>
> ***Audit Service (13.4)** — Every vote event is also consumed by the Audit Service. It stores everything in **S3 with Object Lock** — this means the data cannot be changed or deleted. This supports our requirement for an immutable audit trail."*

---

## 4 - Deployment Diagram — section 5.4

> *"Now, let's look at how we deploy the system.*
>
> *Our code is packaged as **Docker containers**. When a developer pushes code, the container image is built and pushed to **Amazon ECR** — our private container registry.*
>
> *From ECR, the images are deployed to **Amazon EKS**, which is Kubernetes on AWS. Kubernetes manages how many instances of each service are running and automatically scales up or down based on demand.*
>
> *Our services run in **multiple AWS regions**, so if one region has a problem, traffic is automatically redirected to another region via **Global Accelerator**.*
>
> *The database — **PostgreSQL on RDS** — runs in **Multi-AZ** mode, which means there is always a backup instance ready to take over if the primary fails.*
>
> *All configuration and secrets are managed by **AWS KMS** and never stored in code."*

---

## 5 - Top 3 Tradeoffs — sections 6, 6.1

> *"Every architecture has decisions that are hard to change later. We want to highlight our three most important ones."*

### 5.1 - Kafka vs SQS / RabbitMQ — section 6

> *"For the message queue, we chose **Kafka**. The main reason is exactly-once semantics. In a voting system, if a vote is processed twice, that is a serious problem. Kafka gives us the guarantee that each vote event is processed exactly one time. SQS is simpler and cheaper, but it does not give us this guarantee out of the box."*

### 5.2 - PostgreSQL vs NoSQL — section 6

> *"For the database, we chose **PostgreSQL**. This was a deliberate choice because we need **ACID transactions**. ACID means the database will never allow two votes from the same user to be saved — even if two requests arrive at the exact same millisecond. NoSQL databases are faster to scale horizontally, but they give up consistency for speed. In a voting system, consistency is non-negotiable."*

### 5.3 - EKS vs ECS — sections 6, 6.1

> *"For container orchestration, we chose **EKS — Kubernetes**. ECS is simpler and cheaper to operate, but Kubernetes gives us **KEDA** — event-driven autoscaling. This means our pods can scale up automatically when Kafka queue depth increases, which is exactly what happens when millions of users vote at the same time. For 240K requests per second, we need this level of control."*

---

## 6 - API Contracts — section 13.5

> *"Now, let's look at the API contracts. These are the agreements between the frontend, mobile app, and the backend.*
>
> *The main endpoints are:*
>
> - **GET** `/v1/surveys/:id` — returns the survey with all questions and options. The frontend calls this when a user opens a survey.
>
> - **POST** `/v1/surveys/:id/answers` — the user submits an answer. On the first call, the system creates a **SurveyAnswer** session. On subsequent calls, the user sends the `surveyAnswerId` to continue the session.
>
> - **PUT** `/v1/surveys/:id/answers/:answerId/finish` — the user marks the survey as finished. After this, no more answers can be submitted.*
>
> *These contracts are important because the Vote Service is stateless — all session state lives in the `SurveyAnswer` object. The mobile and web apps must follow this flow exactly."*

---

## 7 - Database — sections 10, 10.1

> *"Our database is **PostgreSQL**, running on **Amazon RDS in Multi-AZ** mode. Each microservice has its own database — they do not share tables. This is important for service independence.*
>
> *The most critical part of the schema is the **one-vote enforcement**. We use a globally unique voting token and a unique database constraint. If a user tries to vote twice, the database will reject the second insert — even before the application code can check.*
>
> *We also chose to **not use foreign keys between services**. Instead, relationships between services are managed through events in Kafka. This keeps the services decoupled.*
>
> *For long-term storage and compliance, all vote events are kept in **S3 Glacier** for 7 years."*

---

## 8 - Testing — section 9

> *"Finally, let's talk about testing. We have five layers.*
>
> - **Unit Tests** — We use JUnit. Each function is tested in isolation, with minimal mocks.
>
> - **Integration Tests** — We use JUnit with **Testcontainers** to spin up a real PostgreSQL database in tests. We test API happy paths and error paths.
>
> - **Contract Tests** — We use **Pact** to verify that the API contracts between services are respected. If the Vote Service changes its response format, the contract test will catch it before deployment.
>
> - **Performance Tests** — We use **K6** to simulate thousands of users voting at the same time. This runs in a staging environment that mirrors production.
>
> - **Chaos Tests** — We use **Toxiproxy** to inject failures: network latency, timeouts, dropped connections. We test what happens when an AWS region goes down, or when Kafka is slow. This validates our failover and retry strategies."*

---

## Suggested Slide Order

| Slide | Section in doc | Topic |
|---|---|---|
| 1 | 1.1–1.3, 2, 3 | Introduction — problem and requirements |
| 2 | 5.1, 5.2 | Architecture Diagram — infra flow |
| 3 | 5.3, 13.1–13.4 | Solution Diagram — service interactions |
| 4 | 5.4 | Deployment Diagram — ECR → EKS → Multi-AZ |
| 5 | 6, 6.1 | Top 3 Tradeoffs — Kafka / PostgreSQL / EKS |
| 6 | 13.5 | API Contracts — 3 core endpoints |
| 7 | 10, 10.1 | Database — multi-DB, uniqueness, Glacier |
| 8 | 9 | Testing — 5 layers |
