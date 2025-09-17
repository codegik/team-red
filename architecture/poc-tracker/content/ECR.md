## Amazon ECR (Elastic Container Registry)

**Amazon ECR** is a fully managed **Docker container registry** provided by AWS. It allows developers to store, manage, and deploy container images securely and at scale. ECR integrates seamlessly with AWS services like **ECS**, **EKS**, and **CodeBuild**, enabling efficient container-based workflows.

### Key Features

* 🐳 **Docker-Compatible Registry**: Push, pull, and manage container images using standard Docker CLI or other container tools.
* 🔒 **Secure by Default**: Integrates with **IAM**, **KMS**, and supports **image scanning** for vulnerabilities.
* 🚀 **Seamless CI/CD Integration**: Works with **AWS CodePipeline**, **CodeBuild**, and third-party tools like GitHub Actions, Jenkins, etc.
* 📦 **Private and Public Repositories**: Share images privately within your org or publicly via [ECR Public Gallery](https://gallery.ecr.aws).
* 📤 **Efficient Image Transfers**: Uses **Amazon S3 and EBS** under the hood for fast and reliable storage.
* 🔁 **Replication Across Regions**: Automatically replicate container images to other AWS regions for **low-latency global deployments**.
* 📊 **Lifecycle Policies**: Automatically clean up unused images to save on storage costs.
* 💰 **Pay-as-you-go**: Charged based on **storage** and **data transfer out**.

### Common Use Cases

* 📦 **Store Docker images** for microservices and containerized workloads.
* 🚢 **Deploy containers** to AWS **ECS, EKS, or Fargate** clusters.
* 🔄 **Enable CI/CD pipelines** to build and publish container images.
* 🌍 **Distribute images globally** for multi-region or hybrid cloud setups.
* 🔐 **Scan images** for vulnerabilities before deployment.

👉 In short: **Amazon ECR is AWS’s secure, scalable, and fully managed container image registry designed to streamline your container workflows.**
