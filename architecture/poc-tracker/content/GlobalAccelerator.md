## AWS Global Accelerator

Amazon **Global Accelerator** is a networking service that improves the **availability, performance, and resiliency** of applications with users distributed globally. It uses the AWS global network to route traffic to the optimal endpoint based on **health, geography, and routing policies**.

### Key Features
- 🌍 **Static Anycast IPs**: Provides two fixed IP addresses that act as a single global entry point.  
- ⚡ **Improved Performance**: Routes traffic over the AWS global backbone for lower latency and jitter.  
- ❤️ **Health Checks & Failover**: Monitors application endpoints and automatically redirects traffic if one becomes unhealthy.  
- 🛠️ **Traffic Control**: Supports traffic dials to gradually shift traffic between endpoints.  
- 🔐 **Secure & Scalable**: Integrates with AWS Shield for DDoS protection and scales with demand.  
- ☁️ **AWS Integration**: Works with **ALB, NLB, EC2, EKS, S3, and Elastic IPs**.  

### Common Use Cases
- 🚀 **Global Applications** – Improve performance for users worldwide.  
- 🔄 **Disaster Recovery & Failover** – Automatic rerouting to healthy regions.  
- 🎮 **Gaming & Media** – Low-latency routing for real-time apps.  
- 📦 **E-commerce & SaaS** – Provide a single global entry point for customers.  
- 🛡️ **High Availability** – Keep apps online even during regional failures.  

👉 In short: **Global Accelerator boosts application performance and resilience by using AWS’s global network and static IPs.**
