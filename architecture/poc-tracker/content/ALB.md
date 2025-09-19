## AWS Application Load Balancer (ALB)

**AWS Application Load Balancer (ALB)** is a fully managed Layer 7 load balancer that intelligently routes **HTTP and HTTPS** traffic to your applications based on content, improving scalability, reliability, and performance for modern web architectures.

### Key Features

* 🌐 **Layer 7 Routing**: Content-based routing using **host**, **path**, **headers**, **query strings**, and more.
* 🔁 **Flexible Targeting**: Routes traffic to **EC2**, **ECS**, **Lambda**, **IP addresses**, and **Fargate**.
* ⚖️ **Load Balancing**: Distributes traffic evenly across healthy targets in one or more Availability Zones.
* 🛡️ **AWS WAF Integration**: Protects applications from common web exploits with **built-in WAF support**.
* 🧠 **Smart Health Checks**: Monitors target health and routes only to healthy endpoints.
* 🔒 **SSL Termination**: Offloads SSL/TLS encryption and integrates with **AWS Certificate Manager (ACM)**.
* ⚙️ **Authentication & Authorization**: Supports **OIDC**, **Cognito**, and SAML-based authentication.
* 🧩 **Container Support**: Native integration with **Amazon ECS** using dynamic port mapping.
* 📊 **Detailed Metrics & Logging**: Provides **CloudWatch metrics**, **access logs**, and **request tracing**.

### Common Use Cases

* 🌍 **Modern Web Apps** that require intelligent routing and layer 7 features.
* ☁️ **Microservices architectures** with multiple routes, services, or containerized workloads.
* 🔐 **Secured applications** needing built-in authentication and WAF protection.
* ⚙️ **Lambda-based APIs** using HTTP endpoints or **serverless** backends.
* 🧪 **Blue/Green or Canary deployments** using weighted target groups.

👉 In short: **ALB is the go-to Layer 7 load balancer for routing HTTP(S) traffic intelligently across scalable, modern AWS applications — with built-in security, flexibility, and monitoring.**
