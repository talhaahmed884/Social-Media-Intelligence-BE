# Postman Collection Guide

## Overview

Postman collection for testing the Social Media Intelligence API: user management and linked social accounts
(Instagram).

## Files

| File                                                   | Purpose                                         |
|--------------------------------------------------------|-------------------------------------------------|
| `Social_Media_Intelligence_API.postman_collection.json` | All requests, grouped into folders              |
| `Social_Media_Intelligence.postman_environment.json`    | Variables for local development                 |

## Collection Structure

```
Social Media Intelligence API
├── User Management
│   ├── Register User                 (saves userId)
│   ├── Get User by ID
│   ├── Get User by Email
│   ├── Get All Users
│   ├── Update User Profile
│   ├── Change Password
│   └── Delete User
└── Social Accounts
    ├── Linking
    │   ├── Link Account with Access Token   (saves socialAccountId)
    │   ├── Start Authorization (OAuth)      (saves authorizationUrl)
    │   ├── OAuth Callback (manual replay)   (saves socialAccountId)
    │   ├── List Social Accounts             (saves socialAccountId if unset)
    │   └── Unlink Social Account            (clears socialAccountId)
    └── Platform Data
        ├── Get Posts                        (saves postId)
        ├── Get Post
        └── Get Post Comments
```

---

## Import Instructions

1. Open Postman and click **Import** (top left)
2. Select both files in this folder (`postman/`) and click **Import**
3. Select **Social Media Intelligence - Local** from the environment dropdown (top right)

---

## User Management

| Request             | Method & Path                                  | Notes                                       |
|---------------------|------------------------------------------------|---------------------------------------------|
| Register User       | `POST /api/v1/users/register`                  | Saves `userId`                              |
| Get User by ID      | `GET /api/v1/users/{{userId}}`                 |                                             |
| Get User by Email   | `GET /api/v1/users/by-email?email=...`         | Email is normalized before lookup           |
| Get All Users       | `GET /api/v1/users`                            |                                             |
| Update User Profile | `PUT /api/v1/users/{{userId}}`                 | Email and full name are both required       |
| Change Password     | `PUT /api/v1/users/{{userId}}/change-password` | 401 `CRED_011` if current password is wrong |
| Delete User         | `DELETE /api/v1/users/{{userId}}`              | Also deletes credentials and linked accounts |

**Register body:**

```json
{
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "fullName": "John Doe"
}
```

**Password requirements** (register and new password): 8-72 characters, with at least one uppercase letter, one
lowercase letter, one digit and one special character. The *current* password in Change Password is only checked
against the stored password, not against these rules.

---

## Social Accounts (Instagram)

### Server prerequisites

| Variable                      | Needed for                | Notes                                         |
|-------------------------------|---------------------------|-----------------------------------------------|
| `SOCIAL_TOKEN_ENCRYPTION_KEY` | Any linking               | `openssl rand -base64 32`; without it linking returns 500 `SACC_005` |
| `INSTAGRAM_CLIENT_ID`         | OAuth flow                | Instagram App ID from the Meta App Dashboard  |
| `INSTAGRAM_CLIENT_SECRET`     | OAuth flow                | Instagram App secret                          |
| `INSTAGRAM_REDIRECT_URI`      | OAuth flow                | Must exactly match a redirect URI registered in the dashboard; points at `/api/v1/social-accounts/oauth/instagram/callback` |

Also required:
- The `social_accounts` table from `src/main/resources/schema.sql` exists in Postgres.
- The Instagram account is a **professional** (Business or Creator) account. While the Meta app is in development
  mode, the account must be added as an Instagram tester.

### Quick test with an access token (no OAuth)

1. **User Management > Register User** saves `userId`.
2. Generate a token in the Meta App Dashboard (*Instagram > API setup with Instagram login > Generate token*) and
   paste it into the `instagramAccessToken` environment variable.
3. **Social Accounts > Linking > Link Account with Access Token** saves `socialAccountId`.
4. **Platform Data > Get Posts** saves `postId`, then run **Get Post** and **Get Post Comments**.

### Full OAuth flow

1. **Register User**, then **Start Authorization (OAuth)**. The `authorizationUrl` is printed in the Postman console
   and saved to the environment.
2. Open the URL in a browser and approve access.
3. Instagram redirects the browser to the backend callback, which links the account and shows the JSON result.
4. **List Social Accounts** saves `socialAccountId`, then use **Platform Data** as above.

The authorization link expires after 10 minutes. Use **OAuth Callback (manual replay)** only if you need to replay
a redirect by hand: copy `code` and `state` from the redirect URL into `oauthCode` and `oauthState`. Codes are
single-use.

### Account status

`List Social Accounts` returns `status`:
- **`ACTIVE`**: the stored token works.
- **`REAUTH_REQUIRED`**: Instagram rejected the token. Data requests return 409 `SACC_007` until you link the
  account again, with either flow.

Tokens are refreshed automatically by a daily job and are never returned by the API.

---

## Environment Variables

| Variable               | Description                                   | Auto-populated?                      |
|------------------------|-----------------------------------------------|--------------------------------------|
| `baseUrl`              | API base URL (default: http://localhost:8080) | No                                   |
| `userId`               | UUID of the test user                         | Yes (Register User)                  |
| `platform`             | Platform path value (default: `instagram`)    | No                                   |
| `instagramAccessToken` | Token for the access-token link flow (secret) | No: paste from the Meta dashboard    |
| `socialAccountId`      | ID of the linked account                      | Yes (link, callback, list)           |
| `postId`               | ID of a post for Get Post / Comments          | Yes (Get Posts)                      |
| `authorizationUrl`     | URL to open for OAuth                         | Yes (Start Authorization)            |
| `oauthCode`            | Code for a manual callback replay (secret)    | No                                   |
| `oauthState`           | State for a manual callback replay            | No                                   |

To test against another server, change `baseUrl`: open the environment dropdown, click the eye icon next to the
environment, edit the value and save.

---

## Response Format

### Success

```json
{
    "success": true,
    "message": "Operation successful",
    "data": { ... },
    "timestamp": "2025-12-31T12:00:00"
}
```

### Error

```json
{
    "success": false,
    "errorCode": "USR_012",
    "message": "Invalid input data",
    "errors": ["email: Email must be valid"],
    "timestamp": "2025-12-31T12:00:00"
}
```

`errors` is only present for validation failures. Error code prefixes:
- `USR_` user
- `CRED_` credentials
- `SACC_` social accounts
- `SCnxxx` platform connector
- Common codes such as `INVALID_INPUT`

---

## Common HTTP Status Codes

| Status               | When you'll see it                                                     |
|----------------------|------------------------------------------------------------------------|
| 200 OK               | Successful GET, PUT, DELETE, or OAuth callback                         |
| 201 Created          | Register User, Link Account with Access Token                          |
| 400 Bad Request      | Validation error, malformed ID or `limit`, invalid OAuth state, unknown platform |
| 401 Unauthorized     | Wrong current password (`CRED_011`), Instagram rejected the token (`SC1001`/`SC1003`) |
| 403 Forbidden        | Missing Instagram permission (`SC1004`)                                |
| 404 Not Found        | User, social account or post not found                                 |
| 409 Conflict         | Email already exists (`USR_003`), account needs re-linking (`SACC_007`) |
| 429 Too Many Requests| Rate limit (`SC2001`)                                                  |
| 502 / 503            | Instagram returned an unexpected or invalid response, or was unavailable |
| 500                  | Server-side issue (e.g. missing configuration, database error)         |

---

## Troubleshooting

### "Could not get response"

- Check that the app is running (`mvn spring-boot:run`)
- Verify `baseUrl` and that port 8080 is free

### "User not found" / "Social account not found"

- Run **Register User** (and a link request) first so `userId` / `socialAccountId` are set
- Check the IDs in the environment belong to the same user

### Email already exists

- Use another email, delete the existing user, or generate one in a pre-request script:
  `pm.environment.set("randomEmail", \`test${Date.now()}@example.com\`);`

### Linking returns 401 `SC1001` / `SC1003`

- The token is invalid or expired; generate a new one in the Meta App Dashboard
- The account must be a professional account added as a tester

### Get Posts fails with `SC3001`

- Instagram may reject a requested field (e.g. `caption` is documented as Facebook-Login only); check the server log
  for the Graph error message

### 500 `SACC_005` or `SC7002`

- Set `SOCIAL_TOKEN_ENCRYPTION_KEY` (linking) or the `INSTAGRAM_*` variables (OAuth) and restart the app

---

## Running with Newman (CLI)

```bash
npm install -g newman
newman run postman/Social_Media_Intelligence_API.postman_collection.json \
  -e postman/Social_Media_Intelligence.postman_environment.json \
  --folder "User Management"
```

Run the **Social Accounts** folder only after setting `instagramAccessToken`, because it calls the real Instagram
API.

---

## Support

- Application logs: `logs/application.log`
- Configuration: `src/main/resources/application.yml`
- Social integration details: `SOCIAL_CONNECTOR_GUIDE.md` at the repository root
