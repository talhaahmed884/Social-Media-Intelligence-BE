# user/ — user management domain

CRUD for application users, exposed at `/api/v1/users`. This package is the reference implementation of the
project's vertical-slice layout; copy its structure when adding a new domain.

## Layout

```
controller/   UserController: REST endpoints and the ApiResponse wrapping. No business logic.
service/      UserService: guard clauses → sanitize → validate → business rules → repository
repository/   UserRepository (JpaRepository<User, UUID>): findByEmail, existsByEmail
entity/       User: table `users`, UUID id, email (unique), fullName, created/updated timestamps, @OneToOne credential (cascade ALL)
dto/          RegisterUserDTO, UpdateUserProfileDTO, FindUserByEmailDTO, UserResponseDTO (fromEntity)
validation/   One @Component per input DTO, each wrapping common Validator<T>
sanitization/ One @Component per input DTO, each wrapping common Sanitizer<T>
exception/    UserErrorCode (USR_001..USR_013; next free is USR_014) and UserException extends BaseException
```

`ChangePasswordDTO` and its validator and sanitizer live in `user_credential/`. `UserController` exposes the
endpoint and imports the DTO from there.

## Endpoints (`UserController`)

| Method | Path                         | Service call                                   |
|--------|------------------------------|------------------------------------------------|
| POST   | `/register`                  | `UserService.registerUser` → 201               |
| GET    | `/{userId}`                  | `findUserById`                                 |
| GET    | `/by-email?email=`           | `findUserByEmail` (bound via `@ModelAttribute`) |
| GET    | ``                           | `getAllUsers` (no pagination)                  |
| PUT    | `/{userId}`                  | `updateUserProfile` (email + fullName both required) |
| DELETE | `/{userId}`                  | `deleteUser` (credential and social accounts removed via cascade) |
| PUT    | `/{userId}/change-password`  | `UserCredentialService.changePassword`         |

No endpoint has authentication or authorization yet (JWT and login are on the roadmap).

## Rules enforced

- **Email**: trimmed, lowercased, Gmail/Googlemail dots stripped, and XSS-cleaned; then required, regex format,
  max 255. It must be unique on register and on update. A concurrent duplicate that slips past `existsByEmail` is
  caught by the unique index during `save` + `flush` and returns `USR_003` (409), not a 500.
- **Password** (register): trimmed, then `PasswordStrengthRule.strong()` (8+ characters, with an upper-case letter,
  a lower-case letter, a digit and a special character), at most 72 characters (the BCrypt limit). Stored as BCrypt
  by `user_credential`.
- **Full name**: trimmed, XSS-cleaned, 2–255 characters.
- Registration saves the `User` first, then calls `UserCredentialService.createCredential`, all in one
  `@Transactional`. Credential errors (e.g. `CRED_012`) propagate with their own code.

## Conventions to keep

- Validation and sanitization run in the **service**, not the controller.
- Validation failures throw `UserException(code, result.getFormattedErrors())`. Clients get
  `ErrorResponse.errors` such as `["email: Email must be valid"]`. Rejected values are never echoed back.
- Domain exceptions propagate unchanged. Don't wrap unexpected exceptions in a domain code: a database outage must
  stay a 500 from the GlobalExceptionHandler, not become `USER_NOT_FOUND`. Only translate exceptions you can name
  precisely (e.g. `DataIntegrityViolationException` → `EMAIL_ALREADY_EXISTS`).
- New input DTO → add a matching `XDTOValidator` + `XDTOSanitizer` component, plus tests for both (see
  `src/test/.../CLAUDE.md`). Exclude secrets from Lombok `toString` with `@ToString.Exclude`.

## Gotchas

- Because emails are normalized, `John.Doe@Gmail.com` is stored as `johndoe@gmail.com`. Lookups normalize the same
  way, but responses echo the normalized value.
- `USR_005 REGISTRATION_FAILED`, `USR_010 UPDATE_FAILED` and `USR_013 DELETE_FAILED` are no longer thrown, because
  unexpected failures now surface as 500 `INTERNAL_SERVER_ERROR`. They are kept so existing codes stay reserved.
- Emails are logged at INFO and DEBUG. Consider masking them before production (PII).
