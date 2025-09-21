## OIDC + PKCE (OpenID Connect & Proof Key for Code Exchange)

**OpenID Connect (OIDC)** adds **identity** on top of OAuth 2.0, giving apps a standard way to authenticate users and receive an **ID Token** (JWT). **PKCE** protects the **Authorization Code Flow** in public clients (mobile/SPA) by binding the code exchange to a one‑time secret generated on-device.

### Key Concepts
- 🔐 **ID Token (JWT)**: proves *who* the user is (identity claims like `sub`, `iss`, `aud`, `exp`).
- 🎟️ **Access Token**: allows calling protected APIs (`Authorization: Bearer ...`).
- ♻️ **Refresh Token**: used to obtain new tokens without asking the user to log in again.
- 🧭 **Discovery**: `/.well-known/openid-configuration` gives endpoints & metadata.
- 🧾 **JWKS**: public keys to verify JWT signatures.
- 🧱 **Claims**: `iss`, `aud`, `sub`, `exp`, `iat`, `nonce`, custom claims (e.g., `tenant_id`).
- 🧭 **Scopes**: `openid` (required), plus `profile`, `email`, etc.
- 🧩 **PKCE**: `code_verifier` (random) → `code_challenge` (`S256`), binds the login to the device.
- 🔁 **Authorization Code Flow**: recommended for mobile/SPA (with PKCE).

### How It Works (Happy Path)
1. 📲 **App opens login** in the system browser (Custom Tabs / ASWebAuthenticationSession).  
2. 👤 **User authenticates** in the IdP (e.g., Keycloak).  
3. ↩️ IdP **redirects back** to the app with a short‑lived **authorization code**.  
4. 🔑 App **exchanges code + PKCE code_verifier** at `/token` and receives **ID Token**, **Access Token**, and optionally **Refresh Token**.  
5. 📡 App **calls APIs** with `Authorization: Bearer <access_token>`.  
6. ⏳ On expiry, app **uses the refresh token** to get new tokens.  
7. 🚪 **Logout** clears tokens (and optionally hits the IdP’s logout endpoint).

### Why PKCE?
- 🛡️ Stops **code interception** attacks (the attacker can’t redeem the code without the `code_verifier`).  
- 🔓 Works with **public clients** (no client_secret on device).  
- ✅ Best practice for **mobile/SPA**.

### Security Best Practices
- ✅ Use **Authorization Code + PKCE (S256)** — avoid Implicit Flow.  
- ✅ Validate **JWT signature** (using JWKS), and claims: `iss`, `aud`, `exp`, **`nonce`**, `iat`.  
- ✅ Generate & check **`state`** and **`nonce`**.  
- ✅ Store tokens in **secure storage** (Keychain / EncryptedSharedPreferences).  
- ✅ Don’t log tokens; rotate **refresh tokens**.  
- ✅ Use the **system browser**, not WebView.

### Common Use Cases
- 🔓 **User login** in Android/iOS apps (AppAuth).  
- 🏷️ **Tenant‑aware auth** (e.g., `tenant_id` inside tokens).  
- 🔄 **SSO** across apps from the same IdP.  

### Integration Tips (Keycloak)
- 🏷️ Create a **public client** (Standard Flow ON).  
- 🔁 Configure **Redirect URIs** (e.g., `com.myapp://oauth2redirect`).  
- 🧩 Add **mappers** for custom claims (e.g., `tenant_id`).  
- 🌐 Use **discovery** and **JWKS** for dynamic configuration & verification.

### Client Libraries
- 🤖 **Android (Kotlin/Java)**: AppAuth‑Android  
- 🍎 **iOS (Swift/Obj‑C)**: AppAuth‑iOS  
- ☕ **Java (backend)**: Nimbus JOSE + JWT  
- 🟨 **Node.js**: `jose`

### Pitfalls & Troubleshooting
- 🚫 **WebView** logins quebram SSO e PKCE — use o navegador do sistema.  
- 🧭 `redirect_uri` deve **bater exatamente** com o cadastrado.  
- ⏱️ Erros de **clock skew** podem falhar validação de `exp/iat`.  
- 🔐 Falhas de validação de `nonce/state` → revise armazenamento temporário no app.

👉 In short: **OIDC** proves identity with an **ID Token**, **Access Tokens** call your APIs, and **PKCE** keeps the code flow safe on mobile. Store tokens securely and validate everything.   