# Social Connector Domain - Architecture Guide

## Overview

The Social Connector Domain is a comprehensive **Anti-Corruption Layer** that shields our application from external
social media API changes. It implements multiple design patterns to create a robust, extensible, and maintainable
integration layer.

---

## Design Patterns Implemented

### 1. **Adapter Pattern** 🔌

- **Interface**: `SocialMediaProvider` - Our unified contract
- **Adapters**: `InstagramConnector`, `TwitterConnector` (future)
- **Purpose**: Translate platform-specific APIs to our domain interface
- **Benefit**: Change Instagram's API without touching our application code

### 2. **Template Method Pattern** 📋

- **Abstract Class**: `AbstractSocialConnector`
- **Fixed Algorithm**:
    1. `validateAuth()` - Verify authentication
    2. `checkRateLimit()` - Apply rate limiting
    3. `callExternalApi()` - Platform-specific API call (abstract)
    4. `parseResponse()` - Platform-specific parsing (abstract)
    5. `sanitizeData()` - Clean and validate data
- **Purpose**: Enforce consistent data fetching flow across all platforms
- **Benefit**: Common resilience and sanitization logic in one place

### 3. **Factory Pattern** 🏭

- **Factory**: `SocialConnectorFactory`
- **Purpose**: Centralize connector creation
- **Features**:
    - Simple factory method: `createConnector(SocialPlatform)`
    - Fluent builder API: `forPlatform().withRetryPolicy().build()`
    - Connection pooling: Cached connector instances
- **Benefit**: Easy to add new platforms, consistent configuration

### 4. **Strategy Pattern** 🎯

- **Strategies**: `RateLimiter`, `RetryPolicy`
- **Purpose**: Pluggable resilience mechanisms
- **Policies**:
    - `RetryPolicy.defaultPolicy()` - Standard retry
    - `RetryPolicy.aggressivePolicy()` - More retries for critical ops
    - `RetryPolicy.conservativePolicy()` - Fewer retries for non-critical
- **Benefit**: Different resilience strategies per use case

### 5. **Builder Pattern** 🔨

- **Builders**: All DTOs use `@Builder` annotation
- **Factory Builder**: `ConnectorBuilder` for fluent API
- **Purpose**: Readable object construction
- **Benefit**: Clear, self-documenting code

---

## Architecture Diagram

```
┌──────────────────────────────────────────────────────────┐
│                   Application Layer                       │
│            (Services, Controllers, etc.)                  │
└────────────────────┬─────────────────────────────────────┘
                     │
                     │ Uses Interface
                     ▼
          ┌──────────────────────────┐
          │  SocialMediaProvider     │ ◄─── Our Domain Contract
          │      (Interface)         │
          └──────────────────────────┘
                     △
                     │ Implements
      ┌──────────────┼──────────────┐
      │              │              │
┌─────▼─────┐  ┌────▼─────┐  ┌────▼─────┐
│Instagram  │  │Twitter   │  │Facebook  │  ◄─── Adapters
│Connector  │  │Connector │  │Connector │
└─────┬─────┘  └────┬─────┘  └────┬─────┘
      │             │              │
      │    extends AbstractSocialConnector
      │             │              │
      └─────────────┼──────────────┘
                    │
          ┌─────────▼──────────┐
          │ Template Method    │ ◄─── Enforced Flow
          │ + Sanitization     │
          │ + Rate Limiting    │
          │ + Retry Logic      │
          └────────────────────┘
```

---

## Package Structure

```
social_connector/
├── provider/
│   ├── SocialMediaProvider.java      # Core interface
│   └── SocialPlatform.java           # Platform enum
├── connector/
│   ├── AbstractSocialConnector.java  # Template method
│   └── InstagramConnector.java       # Instagram adapter
├── factory/
│   └── SocialConnectorFactory.java   # Factory + Builder
├── dto/
│   ├── SocialAuthRequest.java
│   ├── SocialAuthResponse.java
│   ├── SocialComment.java
│   └── SocialPost.java
├── exception/
│   ├── SocialConnectorException.java
│   └── SocialConnectorErrorCode.java
└── resilience/
    ├── RateLimiter.java              # Rate limiting strategy
    └── RetryPolicy.java              # Retry strategy
```

---

## Usage Examples

### Basic Usage

```java
// 1. Create connector using factory
SocialMediaProvider instagram = SocialConnectorFactory.createConnector(
                SocialPlatform.INSTAGRAM
        );

// 2. Authenticate
SocialAuthRequest authRequest = SocialAuthRequest.builder()
        .platform(SocialPlatform.INSTAGRAM)
        .accessToken("your-instagram-access-token")
        .build();

SocialAuthResponse authResponse = instagram.connect(authRequest);
System.out.

println("Connected as: "+authResponse.getUsername());

// 3. Fetch comments (Template Method executes)
List<SocialComment> comments = instagram.fetchComments("post-id-123", 50);

// 4. Data is automatically sanitized
comments.

forEach(comment ->{
        System.out.

println("Comment: "+comment.getText());
        System.out.

println("Sanitized: "+comment.isSanitized()); // true
        });

// 5. Disconnect
        instagram.

disconnect();
```

### Advanced Usage with Custom Configuration

```java
// Custom retry policy for critical operations
RetryPolicy aggressiveRetry = RetryPolicy.builder()
                .maxAttempts(5)
                .initialDelayMs(500)
                .backoffMultiplier(2.0)
                .maxDelayMs(60000)
                .build();

// Create connector with custom configuration
SocialMediaProvider instagram = SocialConnectorFactory
        .forPlatform(SocialPlatform.INSTAGRAM)
        .withRetryPolicy(aggressiveRetry)
        .build();
```

### Connection Pooling (Cached Connectors)

```java
// Reuse connector instance across requests
SocialMediaProvider instagram1 = SocialConnectorFactory
                .forPlatform(SocialPlatform.INSTAGRAM)
                .cached()  // ← Reuses existing instance
                .build();

SocialMediaProvider instagram2 = SocialConnectorFactory
        .forPlatform(SocialPlatform.INSTAGRAM)
        .cached()
        .build();

// instagram1 == instagram2 (same instance)
```

### Fetching Posts

```java
// Fetch user's recent posts
List<SocialPost> posts = instagram.fetchPosts("user-id-123", 20);

posts.

forEach(post ->{
        System.out.

println("Post: "+post.getCaption());
        System.out.

println("Likes: "+post.getLikes());
        System.out.

println("Comments: "+post.getCommentCount());
        System.out.

println("Platform: "+post.getPlatform()); // INSTAGRAM
        });
```

### Error Handling

```java
try{
List<SocialComment> comments = instagram.fetchComments("invalid-id", 10);
}catch(
SocialConnectorException e){
        switch(e.

getErrorCode()){
        case RATE_LIMIT_EXCEEDED:
        System.err.

println("Rate limit hit! Wait before retrying");
            break;
                    case AUTHENTICATION_FAILED:
        System.err.

println("Auth failed. Refresh token needed");
            break;
                    case POST_NOT_FOUND:
        System.err.

println("Post not found");
            break;
default:
        System.err.

println("Error: "+e.getFormattedMessage());
        }
        }
```

---

## How It Works: Template Method Flow

When you call `fetchComments()`, here's what happens:

```
1. validateAuth()
   └─> Check if connected
   └─> Check if token expired

2. checkRateLimit()
   └─> Verify we haven't exceeded platform limits
   └─> Apply delay if needed

3. callExternalApi()  ← Platform-Specific (InstagramConnector)
   └─> Build Instagram Graph API URL
   └─> Execute HTTP GET request
   └─> Handle 401, 429, 404, 5xx responses
   └─> Return raw JSON response

4. parseResponse()  ← Platform-Specific (InstagramConnector)
   └─> Parse JSON into SocialComment objects
   └─> Map Instagram fields to our domain model
   └─> Extract id, text, username, timestamp, etc.

5. sanitizeData()  ← Common (AbstractSocialConnector)
   └─> Remove <script> tags
   └─> Remove HTML tags
   └─> Escape HTML entities
   └─> Trim whitespace
   └─> Set sanitized = true

6. Return clean List<SocialComment>
```

---

## Resilience Features

### Rate Limiting

```java
// Automatic rate limiting per platform
RateLimiter rateLimiter = new RateLimiter();

// Instagram: 200 calls/hour
// Twitter: 300 calls/15min
// Facebook: 200 calls/hour
// LinkedIn: 100 calls/day

// Check before API call
rateLimiter.

checkRateLimit(SocialPlatform.INSTAGRAM);

// Get remaining calls
int remaining = rateLimiter.getRemainingRequests(SocialPlatform.INSTAGRAM);
```

### Retry with Exponential Backoff

```java
RetryPolicy policy = RetryPolicy.defaultPolicy();

// Retryable errors:
// - Network errors
// - API call failures (5xx)
// - Platform unavailable

// Non-retryable errors:
// - Authentication failures
// - Rate limit exceeded (should wait instead)
// - Resource not found (404)
// - Invalid credentials

// Automatic retry with backoff:
// Attempt 1: immediate
// Attempt 2: wait 1000ms
// Attempt 3: wait 2000ms
// Attempt 4: wait 4000ms (up to maxDelay)
```

---

## Anti-Corruption Layer Benefits

### 1. **Protection from External Changes**

```
Instagram changes API response format:
└─> Update only InstagramConnector.parseCommentsResponse()
└─> Application code unchanged
```

### 2. **Consistent Domain Model**

```
All platforms return:
└─> SocialComment (our model)
└─> Not InstagramComment, TwitterTweet, etc.
```

### 3. **Unified Error Handling**

```
All connectors throw:
└─> SocialConnectorException (our exception)
└─> With SocialConnectorErrorCode (our codes)
```

### 4. **Centralized Resilience**

```
Rate limiting + Retry logic:
└─> Implemented once in AbstractSocialConnector
└─> Applied to all platforms automatically
```

---

## Adding a New Platform

To add Twitter support:

```java
// 1. Create TwitterConnector.java
public class TwitterConnector extends AbstractSocialConnector {

    @Override
    protected SocialAuthResponse authenticateWithPlatform(SocialAuthRequest request) {
        // Twitter-specific OAuth 2.0 authentication
        // Use Twitter API v2 endpoints
    }

    @Override
    protected String callExternalApiForComments(String postId, int limit) {
        // Call Twitter API: GET /2/tweets/:id/replies
        // Return raw JSON
    }

    @Override
    protected List<SocialComment> parseCommentsResponse(String rawResponse) {
        // Parse Twitter JSON format
        // Map to SocialComment
    }

    // Implement other abstract methods...
}

// 2. Update SocialConnectorFactory
case TWITTER ->new

TwitterConnector(rateLimiter, retryPolicy);

// 3. Done! Ready to use:
SocialMediaProvider twitter = SocialConnectorFactory.createConnector(
        SocialPlatform.TWITTER
);
```

---

## Testing Strategy

### Unit Tests

- Test `RateLimiter` logic
- Test `RetryPolicy` retry attempts
- Test sanitization methods
- Mock external API calls

### Integration Tests

- Test actual Instagram API calls (with test credentials)
- Test rate limit enforcement
- Test retry on transient failures
- Test error code mapping

### Example Test

```java

@Test
void shouldSanitizeXSSAttack() {
    InstagramConnector connector = new InstagramConnector();

    String malicious = "<script>alert('XSS')</script>Hello";
    String sanitized = connector.sanitizeText(malicious);

    assertEquals("Hello", sanitized);
    assertFalse(sanitized.contains("<script>"));
}
```

---

## Production Considerations

### Current Implementation

✅ Synchronous HTTP calls
✅ Thread.sleep () for rate limiting
✅ In-memory rate limit tracking
✅ Basic retry with exponential backoff

### Production Enhancements

🔧 **Resilience4j** - Industry-standard resilience library
🔧 **Circuit Breaker** - Prevent cascading failures
🔧 **Redis** - Distributed rate limiting
🔧 **WebClient** - Reactive non-blocking HTTP
🔧 **Micrometer** - Metrics and monitoring
🔧 **Spring Retry** - `@Retryable` annotations

### Example with Resilience4j

```java
// Add to pom.xml:
// <dependency>
//     <groupId>io.github.resilience4j</groupId>
//     <artifactId>resilience4j-spring-boot3</artifactId>
// </dependency>

@CircuitBreaker(name = "instagram", fallbackMethod = "fallbackFetchComments")
@RateLimiter(name = "instagram")
@Retry(name = "instagram")
public List<SocialComment> fetchComments(String postId, int limit) {
    // Automatic resilience handling
}
```

---

## Key Takeaways

1. **Single Responsibility**: Each class has one reason to change
2. **Open/Closed**: Open for extension (new platforms), closed for modification
3. **Dependency Inversion**: Application depends on abstractions, not concrete adapters
4. **Don't Repeat Yourself**: Common logic in AbstractSocialConnector
5. **Separation of Concerns**: Parsing != Sanitization != Rate Limiting

**Result**: A robust, maintainable, and extensible social media integration layer! 🚀

---

## Quick Reference

| Task             | Code                                                               |
|------------------|--------------------------------------------------------------------|
| Create connector | `SocialConnectorFactory.createConnector(SocialPlatform.INSTAGRAM)` |
| Authenticate     | `connector.connect(authRequest)`                                   |
| Fetch comments   | `connector.fetchComments(postId, limit)`                           |
| Fetch posts      | `connector.fetchPosts(userId, limit)`                              |
| Check connection | `connector.isConnected()`                                          |
| Get rate limit   | `connector.getRateLimitRemaining()`                                |
| Disconnect       | `connector.disconnect()`                                           |
| Handle errors    | `catch (SocialConnectorException e)`                               |

---

**Built with enterprise design patterns and SOLID principles** 🏗️
