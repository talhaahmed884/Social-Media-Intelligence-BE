# social_account/ — social accounts linked to users

Owns the user ↔ social platform link. It stores tokens (encrypted), runs the OAuth flow, refreshes tokens on a
schedule, and exposes data fetches by calling `social_connector` with each account's credentials. It depends on
`social_connector` (platforms) and `user` (`UserService.findUserById`, `User` entity). It must not change either
of them.

## Layout

```
controller/   SocialAccountController (/api/v1/users/{userId}/social-accounts), SocialOAuthCallbackController (/api/v1/social-accounts/oauth)
service/      SocialAccountService (link/unlink/list, OAuth, fetch proxy, refresh), SocialTokenRefreshJob (@Scheduled)
entity/       SocialAccount (table social_accounts), SocialAccountStatus (ACTIVE, REAUTH_REQUIRED)
repository/   SocialAccountRepository (owner-scoped lookups, findTokensDueForRefresh)
crypto/       TokenCipher: AES-256-GCM, base64(IV || ciphertext+tag)
oauth/        OAuthStateService: HMAC-signed, expiring state "base64url(userId|platform|exp|nonce).base64url(mac)"
dto/          LinkSocialAccountDTO (mutable, sanitized/validated), SocialAccountResponseDTO + AuthorizationUrlResponseDTO (records)
validation/ sanitization/   the same per-DTO component pattern as user/
config/       SocialAccountProperties (social-account.*), SocialAccountConfig (@EnableScheduling)
exception/    SocialAccountErrorCode (SACC_001..SACC_007; next free is SACC_008), SocialAccountException
```

## Endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/api/v1/users/{userId}/social-accounts` | List (never includes tokens) |
| POST | `/api/v1/users/{userId}/social-accounts/{platform}/authorize` | Returns `authorizationUrl` to open |
| GET | `/api/v1/social-accounts/oauth/{platform}/callback?code&state` | Redirect target. The user comes from the signed state; `error=` → SACC_004 |
| POST | `/api/v1/users/{userId}/social-accounts/{platform}/token` | Link with an existing token `{accessToken}` → 201 (expiry unknown until first refresh) |
| DELETE | `/api/v1/users/{userId}/social-accounts/{socialAccountId}` | Unlink |
| GET | `/{socialAccountId}/posts?limit=25`, `/{socialAccountId}/posts/{postId}`, `/{socialAccountId}/posts/{postId}/comments?limit=25` | Live pass-through to the connector; nothing is persisted |

`{platform}` is case-insensitive (`instagram`). There is no authentication yet: anyone can call these for any
`userId`, same as the user endpoints.

## Rules

- **Tokens** are encrypted with `TokenCipher` before saving and only decrypted inside the service. They are never
  in DTOs, logs or `toString`. Without `social-account.token-encryption-key` the app starts, but linking fails with
  SACC_005. Changing the key makes stored tokens unreadable, so users must re-link.
- **No HTTP calls inside DB transactions.** The service calls the platform first, then saves.
- **Linking** calls `connector.connect(token)` to learn the platform account ID, then upserts by
  `(user, platform, platformAccountId)` (also a unique constraint) and resets status to ACTIVE.
- **Token rejection gets one retry, at most**: when a fetch fails with `AUTHENTICATION_FAILED` or
  `TOKEN_EXPIRED`, the service reads the current token state with `findTokenState`. That is a DTO projection, so it
  bypasses the open-in-view persistence context and sees what the refresh job or a re-link committed.
  - If the account is still ACTIVE and holds a **different** token (decrypted values are compared, because every
    encryption uses a fresh IV), the call is retried once with that token.
  - Otherwise, or if the retry also fails, the account is set to REAUTH_REQUIRED, and later fetches fail fast with
    SACC_007 (409) until the user links it again.
  - Retrying with a refresh call wouldn't help: Instagram can't refresh an expired or revoked token, so refreshing
    has to happen before expiry (the scheduled job's job).
- **Race-safe writes**: status changes and the refresh job's token writes go through
  `updateStatusIfTokenUnchanged` / `updateTokenIfUnchanged`. These only apply while the stored token is still the
  one the caller used, so a stale copy never overwrites a newer token or flags a freshly refreshed account. Linking
  still uses a normal entity save, so the user's action wins. These bulk updates skip `@UpdateTimestamp`, which is
  why `updated_at` is passed explicitly.
- **Refresh job** (`token-refresh.cron`, UTC, default 03:00): refreshes ACTIVE accounts whose token was issued at
  least `min-token-age` (24h) ago and expires within `refresh-before` (10d), or whose expiry is unknown. Transient
  failures are retried on the next run. It runs on every instance; add ShedLock before scaling out. It is disabled
  in the test profile.
- **OAuth state**: signed with `social-account.oauth.state-secret`. If that is blank, a random secret is
  generated per start, which is fine for a single instance. States expire after `state-ttl` (10m).
- Token timestamps are UTC (from the `Clock` bean). `created_at`/`updated_at` use Hibernate's default zone, like
  the other tables.
- Schema: `social_accounts` in `src/main/resources/schema.sql`. The FK to `users` is `ON DELETE CASCADE`, which
  `@OnDelete` mirrors for H2.

## Next steps

Authentication on these endpoints (JWT roadmap), a frontend redirect after the callback (it currently returns
JSON), persisting fetched posts and comments for analytics, and Postman collection entries.
