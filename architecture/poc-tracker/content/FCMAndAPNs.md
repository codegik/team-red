## FCM & APNs (Push Notifications for Android and iOS)

**FCM (Firebase Cloud Messaging)** and **APNs (Apple Push Notification service)** deliver push notifications to mobile devices. In multi‑tenant apps, combine them with topics (or tags) and your backend (e.g., AWS SNS) to route messages per tenant and feature.

### Key Concepts
- 🔑 **Device Token**: unique token per device/app install (FCM) or device token (APNs).
- 📬 **Registration**: app requests permission (iOS), obtains a token, and sends it to your backend.
- 🧵 **Topics / Tags**: group devices (e.g., `tenant-{id}-dojo`, `tenant-{id}-jobs`).
- 📨 **Notification vs Data Messages**:
  - *Notification*: rendered by the OS (simple payload).  
  - *Data*: custom key/values delivered to the app for flexible handling.
- ⏱️ **TTL / Priority**: delivery expiration and urgency.  
- 🖼️ **Rich Push**: images, actions, deep links (category on iOS).

### Reference Architecture (Project Context)
- 📲 App (Android/iOS) gets the **push token** and sends it to backend.  
- 🗄️ Backend stores token ↔ user ↔ tenant and **subscribes** to tenant topics.  
- 📨 Backend publishes to **SNS** → (platform endpoints) → **FCM/APNs** → devices.  
- 🔗 Push payloads include a **deep link** to app screens (e.g., “Video Job Detail”).

### Quick Start — Android (FCM)
1. 🔧 Add Firebase config & initialize FCM.  
2. 🪪 On startup, **obtain FCM token** and POST to backend with `user_id`/`tenant_id`.  
3. 🧭 Handle messages in foreground/background; open deep links on tap.  
4. 🔁 Listen for **token refresh** events and update backend.

**Sample FCM Data Payload**
```json
{
  "to": "<DEVICE_TOKEN or /topics/tenant-123-jobs>",
  "priority": "high",
  "ttl": "300s",
  "data": {
    "type": "video_job_published",
    "jobId": "d1a2...",
    "deeplink": "myapp://jobs/d1a2"
  },
  "notification": {
    "title": "Your video is ready",
    "body": "Tap to view the annual POC compilation."
  }
}
```

### Quick Start — iOS (APNs)
1. 🔑 Configure push capabilities & **ask for permission**.  
2. 📮 Register for remote notifications → receive **device token** → send to backend.  
3. 🧭 Implement notification handlers (foreground/background) & deep links.  
4. 🔁 Handle token updates (`didRegisterForRemoteNotificationsWithDeviceToken`).

**Sample APNs Payload**
```json
{
  "aps": {
    "alert": {
      "title": "Your video is ready",
      "body": "Tap to view the annual POC compilation."
    },
    "badge": 1,
    "sound": "default",
    "category": "VIDEO_JOB"
  },
  "deeplink": "myapp://jobs/d1a2",
  "type": "video_job_published"
}
```

### Backend Notes (with AWS SNS)
- 🧩 Create **Platform Applications** (one for FCM, one for APNs).  
- 🔗 Create **Platform Endpoints** per device token & subscribe them to topics (e.g., `tenant-{id}-jobs`).  
- 📤 Publish to topic; SNS fans out to FCM/APNs.  
- 🧹 Handle **token invalidation** callbacks to prune stale tokens.

### Security & Privacy
- 🔒 Keep payloads **minimal**; avoid PII in push.  
- 🕵️ Don’t log tokens; encrypt at rest in your DB.  
- 🔁 Refresh tokens on change and handle invalid/expired tokens.  
- 📵 Respect OS rules (Do Not Disturb, notification permission).

### Common Use Cases
- 🧪 **Job status** (e.g., “video published”, “report ready”).  
- 👥 **Dojo invites/updates** in real time.  
- 📣 **Tenant‑level announcements** via topics.  

### Pitfalls & Troubleshooting
- 🔁 **Token rotates** (reinstall, restore, iOS keychain changes) — always resync.  
- 💤 iOS limits background processing; use **silent pushes** (`content-available`) responsibly.  
- 🪟 Foreground behavior: customize in‑app banners for a good UX.  
- 🛑 OEM restrictions (Android) may throttle delivery on some devices.  
- 🖼️ Large images/rich content can be dropped if payload exceeds limits.





👉 In short: **FCM/APNs** deliver cross‑platform push. Register token → send to backend → publish to tenant/topic → open deep link in the app. Keep payloads small and handle token lifecycle carefully.