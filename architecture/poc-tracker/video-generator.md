Architecture Decision Record (ADR): AI Video Generator System

1. Context

This ADR addresses the architectural decisions for an AI-powered video generator system. The system is designed to run on AWS, execute monthly via AWS Batch, and store generated videos in S3. Client information will be sourced from Keycloak.

2. Decision Drivers

The primary drivers for this decision are:

•
Automation: The need to automate video generation on a monthly basis.

•
Scalability: The ability to handle varying workloads for video generation.

•
Cost-effectiveness: Optimizing infrastructure costs for intermittent processing.

•
Security: Secure handling of client data and generated content.

•
Flexibility: Supporting various AI video generation approaches.

3. Considered Options for AI Video Generation

We have identified three primary approaches for AI video generation, each with distinct characteristics, pros, and cons. These methods are based on the input type and the underlying AI models used.

3.1. Text-to-Video Generation

This approach involves generating video content directly from a textual description or script provided by the user. The AI model interprets the text and synthesizes visual elements, scenes, and potentially audio to create a complete video.

Pros:

•
High Creative Control: Users can specify detailed narratives, styles, and themes purely through text, offering immense creative freedom.

•
Efficiency in Content Creation: Eliminates the need for visual assets or pre-recorded footage, streamlining the initial content creation phase.

•
Novel Content Generation: Capable of producing entirely new and unique video content that might be difficult or impossible to capture through traditional filming or editing.

Cons:

•
Computational Intensity: Generating high-quality, coherent video from scratch is resource-intensive, requiring significant computational power and time.

•
Inconsistent Quality: The output quality can vary, often requiring multiple iterations and prompt refinements to achieve desired results.

•
Lack of Specificity: Translating abstract textual descriptions into precise visual outcomes can be challenging, sometimes leading to videos that don't perfectly match the user's vision.

3.2. Image-to-Video Generation

This method uses one or more static images as a primary input to generate a video. The AI can animate elements within the image, create camera movements, or generate a sequence of frames that evolve from the initial image.

Pros:

•
Visual Consistency: Provides a strong visual anchor, ensuring that the generated video maintains elements from the input image, which is beneficial for branding or specific visual themes.

•
Animation and Effects: Excellent for bringing static images to life with dynamic camera movements, subtle animations, or stylistic transformations.

•
Reduced Ambiguity: Starting with a visual input reduces the ambiguity inherent in text-only prompts, often leading to more predictable and controllable outputs.

Cons:

•
Limited Narrative Complexity: May struggle to create complex narratives or significant scene changes that are not implicitly suggested by the initial image.

•
Dependency on Input Image Quality: The quality and style of the generated video are heavily dependent on the input image's characteristics.

•
Creative Constraints: While providing consistency, it can also limit creative freedom compared to pure text-to-video, as the visual foundation is already set.

3.3. Script-to-Video with AI Avatars (or Video-to-Video Transformation)

This approach leverages a script (text) and often incorporates AI-generated avatars or modifies existing video footage. The AI synthesizes speech from the script, animates avatars to lip-sync, and can integrate background visuals or transform existing video content based on instructions.

Pros:

•
Professional Output: Ideal for corporate presentations, educational content, or marketing videos where a consistent presenter or voice is required.

•
Multilingual Support: Can easily generate videos in multiple languages with consistent avatar performance, making it highly valuable for global audiences.

•
High Efficiency for Structured Content: Highly efficient for content that follows a clear script, reducing the need for human actors, filming, and extensive post-production.

•
Cost-Effective for Repetitive Tasks: Significantly lowers costs and time for producing large volumes of similar video content.

Cons:

•
Less Creative Freedom: The use of avatars and structured scripts can limit spontaneous creativity and artistic expression.

•
Potential for Uncanny Valley: AI avatars, while advanced, can sometimes fall into the

uncanny valley, where they appear almost human but subtly off-putting.

•
Dependence on Script Quality: The quality of the final video is heavily reliant on the clarity and completeness of the provided script.

4. Decision: Recommended AI Video Generation Approach

Given the requirement for monthly video generation, potentially with client-specific information, and the need for a scalable and automated solution, the Script-to-Video with AI Avatars approach is recommended.

This decision is based on the following rationale:

•
Automation and Scalability: This method is highly amenable to automation. With a structured script and client data from Keycloak, the system can programmatically generate videos without significant manual intervention. This aligns perfectly with the AWS Batch execution model for monthly runs.

•
Consistency and Professionalism: For client-facing content, consistency in presentation and a professional appearance are crucial. AI avatars ensure a standardized look and feel across all generated videos, regardless of the underlying data.

•
Multilingual Capabilities: If future requirements include generating videos for diverse linguistic audiences, the script-to-video approach with AI avatars offers robust multilingual support, simplifying localization efforts.

•
Integration with Client Data: Client information from Keycloak can be seamlessly integrated into scripts to personalize video content, making each video relevant to the specific client.

•
Reduced Production Overhead: By minimizing the need for human actors, filming, and extensive editing, this approach significantly reduces the operational overhead and costs associated with video production.

While Text-to-Video offers greater creative freedom, its current limitations in consistency and computational cost make it less suitable for a regularly scheduled, automated production pipeline. Image-to-Video is excellent for animating static content but may not provide the narrative depth or personalization required for client communications.

Therefore, the Script-to-Video with AI Avatars approach provides the best balance of automation, quality, scalability, and cost-effectiveness for the proposed system.

5. System Architecture (AWS Components)

The system will leverage the following AWS services:

•
AWS Batch: For orchestrating and executing monthly video generation jobs. This provides managed compute capacity and job scheduling.

•
Amazon S3: For storing input scripts, client data, and the final generated video files. S3 offers high durability, availability, and scalability.

•
AWS Lambda: Potentially used for triggering AWS Batch jobs, pre-processing client data, or post-processing generated videos (e.g., sending notifications).

•
Amazon EC2 (within AWS Batch): The underlying compute instances for video generation tasks, configured with necessary AI/ML libraries and tools.

•
Keycloak: External identity and access management system for client data, integrated securely with the AWS environment.

6. Detailed Implementation Plan (High-Level)

1.
Data Ingestion: Client data from Keycloak will be securely accessed and transformed into structured scripts or templates for video generation.

2.
Video Generation Service: A containerized application (e.g., Docker image) running on AWS Batch will take the processed scripts and utilize an AI video generation SDK/API (implementing the Script-to-Video with AI Avatars approach) to produce video files.

3.
Storage: Generated video files will be uploaded to a designated S3 bucket.

4.
Notification/Delivery: Upon successful generation, a notification mechanism (e.g., AWS SNS/SQS) can trigger further actions, such as sending download links to clients or updating a content management system.

7. Consequences

Positive:

•
Automated, scalable, and cost-effective video production.

•
Consistent brand messaging and professional output.

•
Ability to personalize content for individual clients.

•
Reduced manual effort and human error.

Negative:

•
Initial setup and integration complexity with AI video generation APIs/SDKs.

•
Potential for 'uncanny valley' effect with AI avatars, requiring careful selection of avatar models.

•
Reliance on external AI video generation services, subject to their pricing and API changes.

8. References

[1] How do AI models generate videos? | MIT Technology Review. (2025, September 12). Retrieved from https://www.technologyreview.com/2025/09/12/1123562/how-do-ai-models-generate-videos/
[2] AI Video Generation: What Is It and How Does It Work? (2025, July 23). Colossyan. Retrieved from https://www.colossyan.com/posts/ai-video-generation-what-is-it-and-how-does-it-work
[3] The 15 best AI video generators in 2025 | Zapier. Retrieved from https://zapier.com/blog/best-ai-video-generator/
[4] Top Text-to-Video Tools: Comparing Features, Pros, and Cons | by Talib | Analyst’s corner | Medium. Retrieved from 
