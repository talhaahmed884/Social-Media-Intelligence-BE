# user_credential/ — password storage and verification

Keeps password hashes out of the `User` entity. There is no controller here: password change is exposed through
`user/controller/UserController` (`PUT /api/v1/users/{userId}/change-password`), which delegates to
`UserCredentialService`.

## Layout

```
entity/        UserCredential: table `user_credentials`, shared PK with users (@OneToOne + @MapsId), passwordHash, timestamps
repository/    UserCredentialRepository: findByUser_Id, existsByUser_Id, deleteByUser_Id (underscore = nested property)
service/       UserCredentialService: createCredential, verifyPassword (+ hash upgrade), updatePassword, getCredentialByUserId, changePassword
strategy/      PasswordHashingStrategy (interface, with default needsRehash)
               BCryptHashingStrategy: the only implementation and bean (spring-security-crypto)
dto/ validation/ sanitization/   ChangePasswordDTO + its validator/sanitizer (moved here from user/)
exception/     UserCredentialErrorCode (CRED_001..CRED_013; next free is CRED_014), UserCredentialException
```

Dependencies: `user_credential` → `user` only for the `User` entity (needed by `@MapsId`). `user` → `user_credential`
for registration and the change-password endpoint. Keep it that way, and don't import `user.exception` here.

## Data model

`user_credentials.id` is both the primary key and a foreign key to `users.id` (`ON DELETE CASCADE` in `schema.sql`).
On the JPA side, `User.credential` is `mappedBy = "user"` with `CascadeType.ALL`. Build a credential with
`.user(user)`, not with an id. `@MapsId` copies the id over.

## Hashing

- **BCrypt** with cost `user-credential.bcrypt-strength` (default 12; the test profile uses 4 for speed). Hashes look
  like `$2a$12$...`.
- **Only BCrypt is supported.** A stored value that isn't a BCrypt hash is rejected with
  `CRED_010 INVALID_PASSWORD_HASH`.
  - The old SHA-512 strategy was removed. Any credential still holding a SHA-512 hash can't be verified, and the
    user has to set a new password. There is no reset flow yet.
- **Cost upgrades**: after a **successful** `verifyPassword`, `needsRehash` is true when the stored BCrypt cost is
  lower than `bcrypt-strength`. The password is then re-hashed at the current cost and saved. A failure to upgrade
  is logged and doesn't fail the verification. Today only change-password calls `verifyPassword`; once a login
  endpoint exists, upgrades will happen there too.
- **Adding an algorithm** (e.g. Argon2): implement `PasswordHashingStrategy`, then choose by hash format or an
  explicit `{id}` prefix, the way Spring's `DelegatingPasswordEncoder` does. Keep the old implementation able to
  verify until existing hashes have been upgraded.
- **72-byte limit**: BCrypt only uses the first 72 bytes of a password.
  - Validators cap new passwords at 72 characters.
  - `BCryptHashingStrategy` also rejects passwords over 72 bytes with `CRED_012 PASSWORD_TOO_LONG` (this catches
    multi-byte characters), and never matches one.
- Passwords are encoded as UTF-8. That equals the old default-charset behavior on Java 18+, so existing hashes are
  unaffected.

## changePassword flow

sanitize + validate the DTO. On failure: **`UserCredentialException CRED_013 INVALID_INPUT`**, with field errors in
`ErrorResponse.errors`. Rules:
- `currentPassword` is required only. It is **not** held to the strength rules, so users with older, weaker
  passwords can still change them.
- `newPassword` must be strong and at most 72 characters.

Then:
1. Null check on `userId`.
2. `verifyPassword(current)` (which may upgrade the hash). If it doesn't match: `CRED_011
   CURRENT_PASSWORD_MISMATCH` (401).
3. `updatePassword(new)`.

## Conventions

- Guard clauses at the top of each method.
- `UserCredentialException`s propagate unchanged.
- Hash and verify failures from the strategy become `PASSWORD_HASH_FAILED` / `PASSWORD_VERIFICATION_FAILED`.
- Any other unexpected exception (e.g. a database outage) is **not** caught. It reaches the GlobalExceptionHandler
  as 500, and is never relabelled as "credential not found".
- Some guard-clause codes are historical and kept for compatibility. For example, a null `userId` in
  update/change returns `CRED_006` (500).
