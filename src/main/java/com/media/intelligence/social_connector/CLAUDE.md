# social_connector/ — external social platform integration (Anti-Corruption Layer)

Wraps third-party social APIs behind our own interfaces and DTOs. Callers (today only `social_account/`) see only
`SocialMediaProvider`, `SocialOAuthProvider`, `SocialConnectorFactory`, `SocialPlatform`, the `dto/` classes and
`SocialConnectorException`, never platform JSON. This layer **stores nothing**: tokens and accounts live in
`social_account/`. Human-facing guide: `/SOCIAL_CONNECTOR_GUIDE.md`.

## Status

- Instagram (Instagram API with Instagram Login, `graph.instagram.com/v26.0`) supports data fetches, OAuth code
  exchange and token refresh. It is tested with `MockRestServiceServer` but **has not been run against the live
  API yet**.
- `TWITTER`, `FACEBOOK` and `LINKEDIN` exist only in `SocialPlatform`; the factory throws `UNSUPPORTED_PLATFORM` for
  them.

## Design rules (keep these)

- **Connectors are stateless Spring singletons.** Every fetch takes a `SocialCredentials` record (`platform`,
  `accountId`, `accessToken`, `expiresAt`). `connect(SocialAuthRequest)` only verifies a token and returns the
  account info.
- **OAuth is an optional capability.** A connector also implements `SocialOAuthProvider`
  (`buildAuthorizationUrl(state)`, `exchangeAuthorizationCode(code)` → `SocialToken`, `refreshToken(token)`).
  The factory detects it with `instanceof`.
- **Errors**: `SocialConnectorException extends BaseException`, and `SocialConnectorErrorCode` carries an
  `HttpStatus` and a `retryable` flag. `details` is for logs only. A 401/403 with an `SC1xxx` code means the
  linked social account needs re-authorizing, not that the app session expired.
- **Retries**: only `NETWORK_ERROR` and `PLATFORM_UNAVAILABLE` are retried. When attempts run out, the last
  exception is rethrown unchanged. Never catch a `SocialConnectorException` and rewrap it. Authorization codes are
  **never** retried, because they are single-use.
- **Rate limiting**: a fixed window per `(platform, accountId)`, checked **inside** the retry loop. It is
  in-memory and covers one instance only.
- **Secrets**: tokens go in the URL query, so request URIs are never logged. Spring exceptions that embed the URI
  (such as `ResourceAccessException`) are never attached as causes. Every DTO, record and properties object that
  holds a token or secret masks it in `toString`.
- **Sanitization** uses the common XSS and whitespace rules. Text is cleaned but **not** HTML-escaped.
- **Time**: token expiry and post timestamps are `LocalDateTime` in **UTC**. Get "now" from the injected `Clock`
  bean (UTC), not `LocalDateTime.now()`.

## Layout

```
provider/    SocialMediaProvider (data), SocialOAuthProvider (OAuth capability), SocialPlatform (+ fromValue for path params)
connector/   AbstractSocialConnector<R>: template, generic over the raw response type R (Instagram uses JsonNode)
  instagram/ InstagramConnector (orchestration, @Component, implements both interfaces)
             InstagramApiClient (RestClient: URI building with strict encoding, GET/POST form, error → exception)
             InstagramErrorMapper (Graph body codes → SocialConnectorErrorCode; package-private)
             InstagramResponseParser (JSON → DTOs, timestamps, token responses; static, package-private)
             InstagramProperties (social-connector.instagram.*), InstagramConfig (the "instagramRestClient" bean)
factory/     SocialConnectorFactory: EnumMap registry built from all SocialMediaProvider beans → getConnector / getOAuthProvider
config/      SocialConnectorProperties (social-connector.http/retry/rate-limits), SocialConnectorConfig (RetryPolicy + UTC Clock beans, shared request factory)
resilience/  RateLimiter (defaults + config overrides, injectable Clock), RetryPolicy
dto/         SocialCredentials, SocialToken, SocialAuthRequest/Response, SocialPost, SocialComment
exception/   SocialConnectorErrorCode, SocialConnectorException
```

## Configuration (`application.yml` → `social-connector.*`)

- `http.connect-timeout` and `read-timeout`; `retry.*`; optional `rate-limits.<platform>: {max-requests, window}`.
- `instagram.api-version`, `client-id` / `client-secret` (env `INSTAGRAM_CLIENT_ID` / `INSTAGRAM_CLIENT_SECRET`),
  `redirect-uri` (env `INSTAGRAM_REDIRECT_URI`; must exactly match a URI registered in the Meta dashboard),
  `scopes`, `max-fetch-limit` (50).
- OAuth methods throw `MISSING_CONFIGURATION` (SC7002) when the client ID, secret or redirect URI is not set.
  Fetching with an existing token still works without them.

## Adding a platform

1. Create a `connector/<platform>/` package containing `XConnector extends AbstractSocialConnector<R>` as a
   `@Component` (plus `implements SocialOAuthProvider` if the platform supports OAuth), along with `XApiClient`,
   `XErrorMapper`, `XResponseParser`, `XProperties`, and `XConfig` (its own `RestClient` bean built with
   `SocialConnectorConfig.requestFactory(...)`).
2. Map the platform's error bodies to `SocialConnectorErrorCode`.
3. Add default limits in `RateLimiter.DEFAULT_LIMITS`, and the properties block in `application.yml`.
4. Write tests modelled on `connector/instagram/*Test` (`MockRestServiceServer.bindTo(RestClient.builder())`),
   and register them in `SocialConnectorTestSuite`.

## Instagram specifics (from Meta docs, checked 2026-11)

- OAuth: `www.instagram.com/oauth/authorize` with comma-separated scopes and `state`. The redirect carries `code`
  (valid for 1 hour, single use, may end in `#_`, which we strip), or `error=access_denied`. The code exchange
  (`POST api.instagram.com/oauth/access_token`) returns `{"data":[{access_token,...}]}` for a 1-hour token.
  `ig_exchange_token` then gives a 60-day token. `ig_refresh_token` only works once the token is at least 24 hours
  old and still valid.
- Error codes: `190` means an invalid token (subcode `463` means expired); `4/17/32/341/613/80002` mean rate
  limited; `3/10/200–299` mean a permissions problem; subcode `33` means not found; `1/2` or `is_transient` mean
  temporary. OAuth endpoints use `{error_type, code, error_message}`, which maps to `INVALID_CREDENTIALS`.
- The comments edge returns at most 50 items. Reading `username` on comments needs the
  `instagram_business_manage_comments` scope.
- `caption` on media is documented as Facebook-Login-only, but we request it anyway. **Verify against the live
  API** (TODO in `InstagramConnector`).

## Next steps

Pagination (`paging.cursors`); hashtag and mention extraction; persisting fetched posts and comments for analytics
(a new domain, not this layer); a distributed rate limiter if the app runs as more than one instance; more
platforms.
