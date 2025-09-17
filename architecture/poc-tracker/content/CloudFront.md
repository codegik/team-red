## AWS CloudFront

Amazon **CloudFront** is a Content Delivery Network (CDN) service that speeds up the delivery of websites, APIs, videos, and other web content to users across the globe. It uses a worldwide network of **edge locations** to serve content closer to users, improving **performance, scalability, and security**.

### Key Features
- 🌍 **Global Edge Network**: Delivers content from edge locations nearest to the user.  
- ⚡ **Low Latency & Caching**: Reduces load on origin servers (S3, EC2, ALB, etc.).  
- 🔐 **Security**: Integrated with **AWS Shield**, **WAF**, and supports SSL/TLS.  
- 🛠️ **Customizable**: Control caching with headers, cookies, query strings; use **Lambda@Edge** for logic at the edge.  
- 💰 **Pay-as-you-go**: No upfront costs, charged by data transfer and requests.  

### Common Use Cases
- 🚀 **Static Website Hosting** (HTML, CSS, JS, images).  
- 📺 **Video Streaming** (live & on-demand).  
- 🔄 **API Acceleration** with edge caching.  
- 📦 **Software/File Downloads** at scale.  
- 🛡️ **Security at the Edge** with DDoS and firewall protection.  

👉 In short: **CloudFront delivers content faster, safer, and more reliably using AWS’s global edge network.**
