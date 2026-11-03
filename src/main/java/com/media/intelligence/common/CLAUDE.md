# common/ — shared infrastructure

Code here is used by every domain package. It must not import anything from a domain package (`user`, `user_credential`,
`social_connector`, ...).

## Layout

| Package                        | What it holds                                                                                                                       |
|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `dto/api_response`             | `ApiResponse<T>` + `ApiResponseBuilder<T>`: success envelope `{success:true, message, data, timestamp}`                              |
| `dto/error_response`           | `ErrorResponse` + `ErrorResponseBuilder`: error envelope `{success:false, errorCode, message, errors[], timestamp}`                  |
| `exception`                    | `ErrorCode` interface, `CommonErrorCode` enum, `BaseException`, `ResourceNotFoundException`, `GlobalExceptionHandler`               |
| `validation/core`              | `Validator<T>` (DTO validator), `RuleCombiner<T>` + builder (single-value), `ValidationResult`, `ValidationError`                   |
| `validation/rule`              | `ValidationRule<T>` base + `RequiredRule`, `EmailFormatRule`, `StringLengthRule`, `PasswordStrengthRule` (`strong/medium/basic`)    |
| `sanitization/core`            | `Sanitizer<T>` (DTO sanitizer), `SanitizationCombiner<T>` (single-value), `SanitizationRule<T>` base                                |
| `sanitization/rule`            | `WhitespaceSanitizationRule` (trim), `EmailNormalizationRule` (lowercase + strip Gmail dots), `XssSanitizationRule` (jsoup)          |

## How the pieces fit

- **Responses**: controllers return `ResponseEntity<ApiResponse<T>>` built with `ApiResponse.success(...)`. Errors are
  never built in controllers; they are thrown and turned into `ErrorResponse` by `GlobalExceptionHandler`.
- **Exceptions**: each domain defines an enum implementing `ErrorCode` (code, message, `HttpStatus`) and an exception
  extending `BaseException`. `GlobalExceptionHandler.handleBaseException` reads the code and status off the exception,
  so new domains need no handler changes. Anything that is not a `BaseException` falls through to the catch-all
  handler and returns **500 INTERNAL_SERVER_ERROR**.
- **Validation/sanitization** is a custom framework. Jakarta Bean Validation was removed on purpose (see pom.xml), so
  don't add `@Valid`/`@NotNull`. A domain creates one `@Component` per DTO that wraps a `Validator<T>` or
  `Sanitizer<T>` configured with `.field("fieldName", rule...)`. Services call sanitize first, then validate.

## Gotchas

- `Validator`/`Sanitizer` look up fields **by string name via reflection** (`getDeclaredField`). Renaming a DTO field
  without updating the `.field("...")` string throws `IllegalArgumentException` at runtime. Inherited fields are not
  found.
- Every non-`Required` rule treats `null` as invalid. There is no "optional field" concept yet, so any field with a
  rule is effectively required.
- `Sanitizer` mutates the DTO **in place**. If a rule's type does not match, it swallows the `ClassCastException` and
  leaves the value unchanged.
- `XssSanitizationRule` only runs jsoup when the value looks like it contains an HTML tag. Otherwise it returns the
  value as is.
- `GlobalExceptionHandler.handleBaseException` returns `errorCode` + `message`, plus `errors[]` when the
  exception was built with `BaseException(errorCode, List<String> errors)`. Services pass
  `ValidationResult.getFormattedErrors()` (`"field: message"`, never the rejected value).
- The `MethodArgumentNotValidException` handler is left over from Jakarta validation and is effectively unused.
- `MethodArgumentTypeMismatchException` (for example a malformed UUID path variable or a non-numeric `?limit=`)
  returns 400 `INVALID_INPUT`. Before this handler existed, those requests fell through to the 500 catch-all.
- `BaseException(errorCode, detailMessage, cause)` lets an exception carry internal context in `getMessage()`
  for logs. Clients still only see `errorCode.getMessage()`. `SocialConnectorException` uses this constructor.

## Adding things

- New validation rule: extend `ValidationRule<T>`, call `super(ERROR_CODE, defaultMessage)`, and implement
  `isValid`. Add a test in `src/test/.../common/validation/rule/` and register it in `ValidationSanitizationTestSuite`.
- New sanitization rule: extend `SanitizationRule<T>`, call `super("RULE_NAME")`, and implement `sanitizeValue`
  (it is never called with null).
- New shared error: add it to `CommonErrorCode`. Domain-specific errors belong in the domain's own enum.
