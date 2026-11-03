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
               DelegatingPasswordHashingStrategy: the only bean. New hashes are BCrypt; verify picks the algorithm by hash format
               BCryptHashingStrategy (spring-security-crypto), SHA512HashingStrategy (legacy, verify/upgrade only; not a bean)
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
- **Legacy SHA-512** hashes (`Base64(16-byte salt || SHA-512)`) from before the switch still verify. After a
  **successful** `verifyPassword`, `needsRehash` is true for legacy hashes and for BCrypt hashes with a lower cost,
  so the password is re-hashed with BCrypt and saved. A failure to upgrade is logged and doesn't fail the
  verification.
  - Today only change-password calls `verifyPassword`. Once a login endpoint exists, upgrades will happen there
    too.
  - Accounts that never verify keep their legacy hash, so there is no bulk migration.
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
