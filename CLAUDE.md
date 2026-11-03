# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Social Media Intelligence is a Spring Boot REST backend that will collect and analyze social media data. Today it
has user management (register, CRUD, password change), shared validation, sanitization and error handling, an
Instagram connector layer, and a `social_account` domain. That domain links Instagram accounts to users via OAuth
or an existing token, stores tokens encrypted, refreshes them, and serves live posts and comments.

- **Base package**: `com.media.intelligence`
- **Versions (from pom.xml, the source of truth)**: Spring Boot **3.4.1**, Java **21**, Maven. README badges that
  say "Spring Boot 4.0.1 / Java 25" are wrong.
- **Main dependencies**: spring-boot-starter-web, spring-boot-starter-data-jpa, PostgreSQL (runtime), H2 (tests),
  Lombok, jsoup, commons-text, JUnit 5 + Mockito + `junit-platform-suite`. `owasp encoder` and `aspectjweaver` are
  declared but unused.

## Documentation map: read the nearest CLAUDE.md before editing a package

| Path                                                           | Covers                                                              |
|----------------------------------------------------------------|---------------------------------------------------------------------|
| `src/main/java/com/media/intelligence/common/CLAUDE.md`         | API envelopes, the error-code/exception model, the custom validation and sanitization framework |
| `src/main/java/com/media/intelligence/user/CLAUDE.md`           | User domain, endpoints, rules; the template for new domains         |
| `src/main/java/com/media/intelligence/user_credential/CLAUDE.md`| Password hashing (BCrypt, legacy SHA-512 upgraded on verify), shared-PK entity, change-password flow |
| `src/main/java/com/media/intelligence/social_connector/CLAUDE.md`| Stateless connector design, per-platform subpackages, OAuth capability, error/retry/rate-limit rules, config, how to add a platform, Instagram API notes |
| `src/main/java/com/media/intelligence/social_account/CLAUDE.md` | Linked accounts: endpoints, OAuth flow, token encryption, re-auth status, refresh job |
| `src/test/java/com/media/intelligence/CLAUDE.md`                | Test types, naming, suites, coverage gaps                           |

Human-facing docs: `README.md`, `POSTMAN_GUIDE.md` (plus the two Postman JSON files at the repo root), and
`SOCIAL_CONNECTOR_GUIDE.md`. The README has some stale claims (`application.properties`, Boot 4/Java 25). When
they disagree with the code, trust the code.

### Keeping these notes current (do this as part of every change)

- When you change a package's structure, conventions, endpoints, error codes or known issues, update that package's
  `CLAUDE.md` **in the same change**. Delete notes that are no longer true; don't just add to them.
- When you add a new top-level domain package, give it a `CLAUDE.md` (purpose, layout, conventions, gotchas, status)
  and add it to the table above.
- Record facts that can't be read off the code: intent, invariants, gotchas, status, next steps. Don't paste code or
  line numbers; they go stale.
- Update **Current status** below when a milestone lands or priorities change.

## Architecture in one screen

Vertical slices per domain (`user/`, `user_credential/`, `social_account/`), plus shared `common/`. A domain
normally has `controller/ service/ repository/ entity/ dto/ validation/ sanitization/ exception/`.
`social_connector/` is an infrastructure layer for platform APIs. It has no controllers or tables, and
`social_account/` is its only caller.

Request flow: `Controller` (thin, returns `ResponseEntity<ApiResponse<T>>`) → `Service` (sanitize DTO in place →
validate → business rules → repository; wraps every failure in the domain's `BaseException` subclass) →
`JpaRepository` → PostgreSQL. `common/exception/GlobalExceptionHandler` converts exceptions into `ErrorResponse`,
taking the HTTP status from the exception's `ErrorCode`.

Error-code prefixes: `USR_0xx` (user), `CRED_0xx` (credential), `SACC_0xx` (social account), `SCnxxx` (social
connector), plus shared `CommonErrorCode` names. Every domain's codes implement `ErrorCode`.

## Configuration and database

- `src/main/resources/application.yml`: PostgreSQL via `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD`
  (defaults `localhost:5432/social_media_intelligence`, `postgres` / `postgres`). It sets **`ddl-auto: validate`**,
  and Spring doesn't run `schema.sql` against Postgres by default. **Apply `src/main/resources/schema.sql` to the
  database by hand** before the first run, and keep it in sync with the entities. There is no Flyway or Liquibase.
- `schema.sql` defines `users`, `user_credentials` (shared PK, `ON DELETE CASCADE`), `social_accounts` (FK to
  users, `ON DELETE CASCADE`), and `updated_at` triggers.
- Social settings live under `social-connector.*` and `social-account.*` in `application.yml`. Environment
  variables: `SOCIAL_TOKEN_ENCRYPTION_KEY` (required to link accounts; generate with `openssl rand -base64 32`),
  `INSTAGRAM_CLIENT_ID`, `INSTAGRAM_CLIENT_SECRET` and `INSTAGRAM_REDIRECT_URI` (for OAuth), and
  `SOCIAL_OAUTH_STATE_SECRET` (when running more than one instance).
- `user-credential.bcrypt-strength` (default 12) sets the BCrypt cost. The test profile uses 4 to keep tests fast.
- `Clock` is a UTC bean (in `SocialConnectorConfig`). Inject it instead of calling `LocalDateTime.now()` in new
  code.
- Logs go to the console and to `logs/application.log`. Hibernate SQL and bind parameters are logged at
  DEBUG/TRACE, even in the main profile.
- Tests use the `test` profile (`src/test/resources/application-test.yml`): H2 in PostgreSQL mode, `create-drop`.
- `spring-boot-docker-compose` is on the classpath, but the repo has no `compose.yaml`.

## Build and Development Commands

```bash
mvn clean install                      # build + tests
mvn spring-boot:run                    # run (needs Postgres with schema.sql applied)
mvn test                               # all tests (H2, no Postgres needed)
mvn test -Dtest=UserServiceTest        # one class
mvn test -Dtest=ClassName#methodName   # one method
mvn test -Dtest=AllTestsSuite          # the JUnit suite
mvn package                            # jar in target/
```

The Spring Boot configuration processor and Lombok are set up as annotation processors in `maven-compiler-plugin`;
both are required for compilation.

## Current status (update when this changes)

- Done: user CRUD and password change with sanitization, validation and tests; common response, exception and
  validation framework; Postman collection.
- Done (not yet tried against the live Instagram API): the `social_connector` layer (Instagram via RestClient,
  OAuth, config properties) and the `social_account` domain (link via OAuth or token, encrypted tokens, scheduled
  refresh, REAUTH_REQUIRED status, live fetch endpoints).
- Next: run against the real Instagram API with a Meta app, persist fetched posts and comments for analytics, add
  pagination, add Postman entries for the social endpoints.
- Done: BCrypt password hashing (legacy SHA-512 hashes are upgraded on the next successful verification),
  field-level validation errors in `ErrorResponse.errors`, and change-password classes moved to `user_credential`.
- Not started (roadmap in README): JWT auth and login (no endpoint is protected today), more platforms, scheduled
  data collection, analytics.
