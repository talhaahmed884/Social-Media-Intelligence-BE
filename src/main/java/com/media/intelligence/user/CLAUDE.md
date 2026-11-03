# user/ — user management domain

CRUD for application users, exposed at `/api/v1/users`. This package is the reference implementation of the
project's vertical-slice layout; copy its structure when adding a new domain.

## Layout

```
controller/   UserController: REST endpoints and the ApiResponse wrapping. No business logic.
service/      UserService: sanitize → validate → business rules → repository, all inside try/catch → UserException
repository/   UserRepository (JpaRepository<User, UUID>): findByEmail, existsByEmail
entity/       User: table `users`, UUID id, email (unique), fullName, created/updated timestamps, @OneToOne credential (cascade ALL)
dto/          RegisterUserDTO, UpdateUserProfileDTO, FindUserByEmailDTO, ChangePasswordDTO, UserResponseDTO (fromEntity)
validation/   One @Component per input DTO, each wrapping common Validator<T>
sanitization/ One @Component per input DTO, each wrapping common Sanitizer<T>
exception/    UserErrorCode (USR_001..USR_013; next free is USR_014) and UserException extends BaseException
```

## Endpoints (`UserController`)

| Method | Path                         | Service call                                   |
|--------|------------------------------|------------------------------------------------|
| POST   | `/register`                  | `UserService.registerUser` → 201               |
| GET    | `/{userId}`                  | `findUserById`                                 |
| GET    | `/by-email?email=`           | `findUserByEmail` (bound via `@ModelAttribute`) |
| GET    | ``                           | `getAllUsers` (no pagination)                  |
| PUT    | `/{userId}`                  | `updateUserProfile` (email + fullName both required) |
| DELETE | `/{userId}`                  | `deleteUser` (credential removed via cascade)  |
| PUT    | `/{userId}/change-password`  | `UserCredentialService.changePassword`         |

No endpoint has authentication or authorization yet (JWT and login are on the roadmap).

## Rules enforced

- Email: trimmed, lowercased, Gmail/Googlemail dots stripped, XSS-cleaned; then required, regex format, max 255.
  Must be unique on register and on update.
- Password (register): trimmed, then `PasswordStrengthRule.strong()` (8+ chars, upper, lower, digit, special
  character).
- Full name: trimmed, XSS-cleaned, 2–255 characters.
- Registration saves the `User` first, then calls `UserCredentialService.createCredential`, all in one
  `@Transactional`.

## Conventions to keep

- Validation and sanitization run in the **service**, not the controller.
- Services catch `UserException` and rethrow it unchanged, and wrap any other exception in a `UserException` with a
  suitable code. Keep this pattern so clients never see raw exceptions.
- New input DTO → add a matching `XDTOValidator` + `XDTOSanitizer` component and tests for both (see
  `src/test/.../CLAUDE.md`).

## Gotchas

- Any validation failure becomes a generic `USR_012 INVALID_INPUT` (or `USR_004` for find-by-email). The per-field
  messages are only logged.
- Because emails are normalized, `John.Doe@Gmail.com` is stored as `johndoe@gmail.com`. Lookups normalize the same
  way, but responses echo the normalized value.
- **Cross-domain coupling**: `ChangePasswordDTO`, `ChangePasswordDTOValidator` and `ChangePasswordDTOSanitizer` live
  here but are used by `user_credential/service/UserCredentialService`. `UserService` and `UserController` in turn
  depend on `UserCredentialService`. Keep that in mind before moving classes between the two packages.
- `ChangePasswordDTOValidator` also applies `strong()` to `currentPassword`, so a user whose stored password is weaker
  can't change it.
