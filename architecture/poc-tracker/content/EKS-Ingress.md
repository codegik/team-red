## AWS EKS Ingress

In **Amazon EKS**, an **Ingress** is a Kubernetes resource that manages **external access** to services running inside the cluster, typically over HTTP or HTTPS.  
It provides a single entry point to route traffic to different services based on rules like hostname or URL path.

### Key Features
- 🌍 **Single Entry Point** – Consolidates access to multiple services under one DNS name.  
- ⚖️ **Routing Rules** – Directs traffic by **hostnames** (e.g., `api.example.com`) or **paths** (e.g., `/api`, `/web`).  
- 🔐 **TLS/SSL Termination** – Supports HTTPS for secure communication.  
- 📦 **Ingress Controllers** – Requires a controller to implement rules (e.g., **AWS Load Balancer Controller**, NGINX, Traefik, Istio).  
- ☁️ **AWS Integration** – With the **AWS Load Balancer Controller**, Ingress can automatically provision an **Application Load Balancer (ALB)** or **Network Load Balancer (NLB)**.  

### Common Use Cases
- 🚀 **Expose Services** – Make Kubernetes services accessible from outside the cluster.  
- 🔄 **Path-Based Routing** – `/api → service-a`, `/web → service-b`.  
- 🌍 **Multi-Domain Hosting** – Serve multiple hostnames from a single EKS cluster.  
- 🔐 **Secure Traffic** – Enforce HTTPS and integrate with AWS Certificate Manager (ACM).  
- ⚡ **Scalable Apps** – Route traffic through AWS-managed load balancers for high availability.  

### Benefits
- ✅ Simplifies external access management to services.  
- ✅ Centralized traffic routing and SSL management.  
- ✅ Native AWS load balancer integration for production-grade availability.  
- ✅ Works with existing Kubernetes tools and manifests.  

👉 In short: **Ingress in EKS provides a unified, secure, and scalable way to expose Kubernetes services to the outside world using AWS load balancers.**
