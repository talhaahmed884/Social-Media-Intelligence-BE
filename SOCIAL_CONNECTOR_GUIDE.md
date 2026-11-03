# Social Connector & Social Accounts - Guide

## Overview

Two packages work together:

| Package             | Role                                                                                          |
|---------------------|-----------------------------------------------------------------------------------------------|
| `social_connector/` | **Anti-Corruption Layer** for platform APIs: HTTP, JSON mapping, OAuth, errors, retry, rate limits. Stores nothing. |
| `social_account/`   | Links platform accounts to users, stores tokens **encrypted**, refreshes them, exposes REST endpoints. |

Status: Instagram is implemented and tested with mocked HTTP. It has **not been run against the live Instagram API
yet**.

---

## Quick Start: Link an Instagram Account

### 1. Configure

Set the following environment variables (or the matching keys in `application.yml`):

| Variable                       | Needed for       | Notes                                                        |
|--------------------------------|------------------|--------------------------------------------------------------|
| `SOCIAL_TOKEN_ENCRYPTION_KEY`  | Linking at all   | `openssl rand -base64 32`. Changing it invalidates stored tokens. |
| `INSTAGRAM_CLIENT_ID`          | OAuth flow       | Instagram App ID (Meta App Dashboard → Instagram → API setup with Instagram login) |
| `INSTAGRAM_CLIENT_SECRET`      | OAuth flow       | Instagram App secret                                         |
| `INSTAGRAM_REDIRECT_URI`       | OAuth flow       | Must **exactly** match a redirect URI registered in the dashboard. Default: `http://localhost:8080/api/v1/social-accounts/oauth/instagram/callback` |
| `SOCIAL_OAUTH_STATE_SECRET`    | Multi-instance   | Optional for a single instance                               |

Apply the new `social_accounts` table from `src/main/resources/schema.sql` to Postgres.

The account must be an Instagram **professional** (Business or Creator) account. Meta may require an HTTPS redirect
URI; for local testing, a tunnel (for example ngrok) pointing at the callback works.

### 2a. Link via OAuth (normal flow)

```http
POST /api/v1/users/{userId}/social-accounts/instagram/authorize
→ { "data": { "platform": "INSTAGRAM", "authorizationUrl": "https://www.instagram.com/oauth/authorize?..." } }
```

Open `authorizationUrl` in a browser and approve. Instagram then redirects to:

```http
GET /api/v1/social-accounts/oauth/instagram/callback?code=...&state=...
→ { "data": { "id": "...", "platform": "INSTAGRAM", "username": "...", "status": "ACTIVE", "tokenExpiresAt": "..." } }
```

The backend exchanges the code for a short-lived token, upgrades it to a 60-day token, and stores it encrypted.

### 2b. Link with an existing token (quick testing)

Generate a token in the Meta App Dashboard, then:

```http
POST /api/v1/users/{userId}/social-accounts/instagram/token
{ "accessToken": "IGAA..." }
→ 201 { "data": { "id": "...", "status": "ACTIVE", "tokenExpiresAt": null } }
```

The expiry stays unknown until the first scheduled refresh.

### 3. Use it

| Method | Path (prefix `/api/v1/users/{userId}/social-accounts`) | Returns                      |
|--------|---------------------------------------------------------|------------------------------|
| GET    | ``                                                      | Linked accounts (no tokens)  |
| GET    | `/{socialAccountId}/posts?limit=25`                     | The account's media (≤ 50)   |
| GET    | `/{socialAccountId}/posts/{postId}`                     | One post                     |
| GET    | `/{socialAccountId}/posts/{postId}/comments?limit=25`   | Comments (≤ 50)              |
| DELETE | `/{socialAccountId}`                                    | Unlink                       |

Data is fetched live and not persisted yet.

---

## Token Lifecycle

```
OAuth code ──exchange──► short-lived (1h) ──ig_exchange_token──► long-lived (60 days)
                                                                       │
                     SocialTokenRefreshJob (daily, 03:00 UTC) ◄────────┘
                     refreshes ACTIVE tokens ≥ 24h old that expire within 10 days (or have unknown expiry)
```

When the platform rejects a token, the backend checks whether the stored token changed while the request was in
flight (for example, the refresh job just rotated it). If it did, the request is retried **once** with the new
token. Otherwise (revoked, expired, password changed) the account becomes `REAUTH_REQUIRED`, and fetches return
**409 `SACC_007`** until the user links the account again, using either link flow.

Instagram can't refresh a token that is already expired or revoked, so refreshing after an error would not help.
That is why refresh happens on a schedule, before expiry.

---

## Errors

All errors use the standard `ErrorResponse` (`errorCode`, `message`):

| Situation                                        | Code                 | HTTP |
|--------------------------------------------------|----------------------|------|
| Social token invalid / expired                   | `SC1001` / `SC1003`  | 401  |
| Missing Instagram permission                     | `SC1004`             | 403  |
| Local or platform rate limit                     | `SC2001`             | 429  |
| Post not found                                   | `SC4001`             | 404  |
| Platform down / network (after retries)          | `SC5002` / `SC3004`  | 503  |
| Unknown platform in path                         | `SC5001`             | 400  |
| OAuth not configured                             | `SC7002`             | 500  |
| Social account not found / not yours             | `SACC_001`           | 404  |
| Invalid or expired authorization link            | `SACC_003`           | 400  |
| User denied access on Instagram                  | `SACC_004`           | 400  |
| Encryption key not set                           | `SACC_005`           | 500  |
| Account needs re-linking                         | `SACC_007`           | 409  |

An `SC1xxx` 401 means the **social account** needs re-authorization, not the app session.

---

## Connector Architecture

| Pattern             | Where                                                    |
|---------------------|----------------------------------------------------------|
| **Adapter**         | `SocialMediaProvider` / `SocialOAuthProvider` ← `InstagramConnector` |
| **Template Method** | `AbstractSocialConnector<R>.fetch(...)`: validate → (rate limit + call) with retry → parse → sanitize |
| **Registry**        | `SocialConnectorFactory`: Spring collects connector beans; OAuth support is detected by interface |
| **Strategy**        | `RetryPolicy`, `RateLimiter` (configurable)              |

```
social_connector/
├── provider/      SocialMediaProvider, SocialOAuthProvider, SocialPlatform
├── connector/     AbstractSocialConnector<R>
│   └── instagram/ InstagramConnector, InstagramApiClient (RestClient), InstagramErrorMapper,
│                  InstagramResponseParser, InstagramProperties, InstagramConfig
├── factory/       SocialConnectorFactory
├── config/        SocialConnectorProperties, SocialConnectorConfig
├── resilience/    RateLimiter, RetryPolicy
├── dto/           SocialCredentials, SocialToken, SocialAuthRequest/Response, SocialPost, SocialComment
└── exception/     SocialConnectorErrorCode, SocialConnectorException
```

Connectors are **stateless**. Every call takes `SocialCredentials`, so one bean serves all users.
`SocialAccountService` loads and decrypts the account's token and passes it in.

### Configuration (`social-connector.*`)

```yaml
social-connector:
  http: { connect-timeout: 10s, read-timeout: 30s }
  retry: { max-attempts: 3, initial-delay: 1s, backoff-multiplier: 2.0, max-delay: 30s }
  rate-limits:            # optional overrides of the local per-account guard
    instagram: { max-requests: 200, window: 1h }
  instagram:
    api-version: v26.0
    client-id: ...
    client-secret: ...
    redirect-uri: ...
```

---

## Adding a New Platform

1. Create `connector/<platform>/` with a `@Component XConnector extends AbstractSocialConnector<R>`, adding
   `implements SocialOAuthProvider` if the platform supports OAuth. Put the HTTP client, error mapper, parser,
   properties and config classes next to it, following the Instagram files.
2. Map the platform's error bodies to `SocialConnectorErrorCode`. Only network and unavailable errors should be
   retryable.
3. Add default rate limits and an `application.yml` block.
4. Add tests using `MockRestServiceServer` (no network).

The factory and `social_account` endpoints pick up the new platform automatically through `{platform}` in the
path.

---

## Instagram Notes

- API: Instagram API with Instagram Login, `https://graph.instagram.com/v26.0`. Scopes:
  `instagram_business_basic` and `instagram_business_manage_comments`.
- The token is sent as the `access_token` query parameter (the documented method). Request URIs are never logged.
- Errors are mapped from the Graph error body (`error.code` / `error_subcode`), not just the HTTP status.
- The comments edge returns at most 50 per request.
- `caption` on media is documented as Facebook-Login-only. Verify it against the live API.
