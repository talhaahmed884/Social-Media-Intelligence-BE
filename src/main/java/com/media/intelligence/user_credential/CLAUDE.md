# user_credential/ — password storage and verification

Keeps password hashes out of the `User` entity. No controller of its own: password change is exposed through
`user/controller/UserController` (`PUT /api/v1/users/{userId}/change-password`).

## Layout

```
entity/      UserCredential: table `user_credentials`, shared PK with users (@OneToOne + @MapsId), passwordHash, timestamps
repository/  UserCredentialRepository: findByUser_Id, existsByUser_Id, deleteByUser_Id (underscore = nested property)
service/     UserCredentialService: createCredential, verifyPassword, updatePassword, getCredentialByUserId, changePassword
strategy/    PasswordHashingStrategy (interface) and SHA512HashingStrategy (the only @Component implementation)
exception/   UserCredentialErrorCode (CRED_001..CRED_011; next free is CRED_012) and UserCredentialException extends BaseException
```

## Data model

`user_credentials.id` is both the primary key and a foreign key to `users.id` (`ON DELETE CASCADE` in `schema.sql`).
On the JPA side, `User.credential` is `mappedBy = "user"` with `CascadeType.ALL`. Build a credential with
`.user(user)`, not with an id. `@MapsId` copies the id over.

## Hashing

- `SHA512HashingStrategy` produces `Base64(16-byte random salt || SHA-512(salt + password))` and verifies with
  `MessageDigest.isEqual` (constant-time comparison).
- **The README says BCrypt, but that is wrong**: the code uses salted SHA-512, a fast hash that is weak against brute
  force. The planned fix is a `BCryptHashingStrategy` or `Argon2HashingStrategy` that implements
  `PasswordHashingStrategy`. Because there is exactly one bean, adding a second implementation needs `@Primary` or a
  qualifier. Existing hashes would also need a migration path, for example detecting the hash format in `verify`.
- `password.getBytes()` uses the platform's default charset. Use `StandardCharsets.UTF_8` when changing this code,
  and keep it compatible with existing hashes.

## changePassword flow

sanitize + validate the DTO (on failure it throws a **`UserException`** `INVALID_INPUT`, not a credential error) →
null check on `userId` → `verifyPassword(current)` → if it doesn't match, `CRED_011 CURRENT_PASSWORD_MISMATCH` (401)
→ `updatePassword(new)`.

## Conventions

Same as `user/`: guard clauses at the top, nested try/catch that rethrows `UserCredentialException` unchanged, and
any other exception wrapped in a `UserCredentialException` with a specific code.
