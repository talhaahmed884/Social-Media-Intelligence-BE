# src/test — test conventions

The tree mirrors `src/main/java/com/media/intelligence/`. Test config is `src/test/resources/application-test.yml`:
H2 in-memory database in PostgreSQL mode, `ddl-auto: create-drop`, `schema.sql` **not** executed, H2 console
enabled. Activate it with `@ActiveProfiles("test")`.

## Kinds of tests

| Suffix / annotation                                  | Use for                                                        | Example                                  |
|------------------------------------------------------|----------------------------------------------------------------|------------------------------------------|
| `*Test` + `@ExtendWith(MockitoExtension.class)`       | Service logic with mocked repositories, validators and sanitizers | `UserServiceTest`, `UserCredentialServiceTest` |
| `*Test` + `@WebMvcTest(X.class)` + `@MockitoBean`     | Controller status codes and JSON shape                         | `UserControllerTest`                     |
| `*Test`, plain `new`                                  | Rules, DTO validators and sanitizers (no Spring)               | `common/.../*RuleTest`, `user/validation/*Test` |
| `*IntegrationTest` + `@DataJpaTest`                   | Repository queries and constraints against H2                  | `UserRepositoryIntegrationTest`          |
| `*IntegrationTest` + `@SpringBootTest` (+ `@AutoConfigureMockMvc`) | Full stack, HTTP to DB                            | `UserControllerIntegrationTest`, `UserServiceIntegrationTest` |

Style used throughout: `@DisplayName("methodOrEndpoint: Should ...")`, method names like
`registerUser_ShouldSucceed`, `// Arrange / Act / Assert` comments, and builders for fixtures. Integration tests
clean up with `credentialRepository.deleteAll(); userRepository.deleteAll();` in `@BeforeEach` (credentials first,
because of the FK).

## Suites

`AllTestsSuite` → `common/ValidationSanitizationTestSuite`, `user/UserIntegrationTestSuite`,
`user_credential/UserCredentialIntegrationTestSuite`, `social_connector/SocialConnectorTestSuite`,
`social_account/SocialAccountTestSuite` (JUnit Platform `@Suite @SelectClasses`). **Add new test classes to the
relevant suite by hand.** `mvn test` (Surefire) finds `*Test` classes on its own, so a missing entry only
affects runs of a suite. The test counts in the suite Javadocs are approximate and out of date. The groupings are
loose: user DTO validator and sanitizer tests live in the common suite, and `UserCredentialServiceTest` is in the
user suite.

## Social domain testing patterns

- **No network**: connector tests bind `MockRestServiceServer` to a `RestClient.Builder` **before** building the
  client (`MockRestServiceServer.bindTo(builder).build()`, then `new InstagramApiClient(builder.build(), ...)`). Use
  `ExpectedCount.times(n)` to assert retry counts, and `withException(new IOException())` for network failures.
- Use a `RetryPolicy` with `initialDelayMs(0)` (the test profile also sets `social-connector.retry.initial-delay:
  0s`) and a fixed `Clock`; `RateLimiterTest` uses a `MutableClock`.
- `SocialAccountControllerIntegrationTest` replaces `SocialConnectorFactory` with `@MockitoBean` and returns
  Mockito mocks of `SocialMediaProvider` / `SocialOAuthProvider`. This keeps the real HTTP → service → H2 path but
  never calls a platform. Clean up with `socialAccountRepository.deleteAll()` first, then credentials, then users.
- The test profile sets a fixed test-only `social-account.token-encryption-key` and `oauth.state-secret`, dummy
  Instagram client settings, and `token-refresh.enabled: false`. Shared values for unit tests are in
  `social_account/SocialAccountTestFixtures`.

## Coverage gaps

- `common/validation/core` and `common/sanitization/core` (`Validator`, `Sanitizer`, `RuleCombiner`) and
  `GlobalExceptionHandler` are only covered indirectly.
- After moving or renaming a class, run `mvn clean test`. Stale `.class` files in `target/` can register a second
  bean with the same name and break every Spring context.
- There is no plain context-load test (a `SocialMediaIntelligenceApplicationTests`-style smoke test).
