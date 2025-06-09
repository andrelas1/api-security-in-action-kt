# Rate Limiter Implementation

This document describes the rate limiter implementation for the Natter API application.

## Overview

The rate limiter is implemented using Google Guava's `RateLimiter` class and applied to all API endpoints through a Servlet Filter. It enforces a limit of **5 requests per second** per client IP address.

## Implementation Details

### Components

1. **RateLimitFilter** - The main filter that intercepts all requests
2. **FilterConfig** - Configuration to register the filter for all `/api/*` endpoints
3. **EnhancedRateLimiter** - Custom wrapper around Guava's RateLimiter with usage tracking
4. **Guava RateLimiter** - Underlying thread-safe rate limiting implementation

### How It Works

1. **Client Identification**: Each client is identified by their IP address, with support for proxy headers:
   - `X-Forwarded-For` (takes the first IP if multiple)
   - `X-Real-IP`
   - Remote address as fallback

2. **Rate Limiting**: Uses Guava's token bucket algorithm with enhanced tracking:
   - Each client gets their own `EnhancedRateLimiter` instance
   - Rate is set to 5.0 requests per second
   - Tracks remaining requests and reset times
   - Requests are either allowed immediately or rejected

3. **Response Handling**:
   - **Allowed requests**: Pass through with rate limit headers
   - **Blocked requests**: Return HTTP 429 with error details

### HTTP Headers

#### For All Requests
- `X-RateLimit-Limit`: Maximum requests per second (5)
- `X-RateLimit-Remaining`: Number of remaining requests in current window
- `X-RateLimit-Reset`: Timestamp when the rate limit window resets

#### For Blocked Requests (Additional)
- `Retry-After`: Seconds to wait before retrying (1)

### Error Response Format

When rate limit is exceeded, the API returns:

```json
{
    "error": "Rate limit exceeded",
    "message": "Too many requests. Maximum 5 requests per second allowed.",
    "retryAfter": 1
}
```

## Configuration

### Dependencies

The implementation uses the following dependency:

```gradle
implementation 'com.google.guava:guava:32.1.3-jre'
```

### Filter Registration

The filter is registered in `FilterConfig.kt`:

```kotlin
@Bean
fun rateLimitFilterRegistration(rateLimitFilter: RateLimitFilter): FilterRegistrationBean<RateLimitFilter> {
    val registration = FilterRegistrationBean<RateLimitFilter>()
    registration.filter = rateLimitFilter
    registration.addUrlPatterns("/api/*")
    registration.setName("rateLimitFilter")
    registration.order = 1
    return registration
}
```

## Testing

The implementation includes comprehensive tests:

### Unit Tests (`RateLimitFilterTest`)
- Verifies requests within limits are allowed
- Confirms excessive requests are blocked
- Tests IP address detection from headers
- Validates error response format

### Integration Tests (`RateLimitIntegrationTest`)
- End-to-end testing with real HTTP requests
- Concurrent request handling
- Rate limit reset behavior
- Both GET and POST request testing

## Usage Examples

### Normal Request
```bash
curl -i http://localhost:8080/api/spaces
# Returns: 200 OK with rate limit headers:
# X-RateLimit-Limit: 5
# X-RateLimit-Remaining: 4
# X-RateLimit-Reset: 1640995200000
```

### Rapid Requests (Triggering Rate Limit)
```bash
for i in {1..10}; do
  curl -i http://localhost:8080/api/spaces
done
# First 5 requests: 200 OK with decreasing X-RateLimit-Remaining values
# Subsequent requests: 429 Too Many Requests with X-RateLimit-Remaining: 0
```

## Benefits

1. **Protection**: Prevents API abuse and DoS attacks
2. **Fairness**: Ensures equal access for all clients
3. **Performance**: Maintains system stability under load
4. **Transparency**: Real-time feedback through detailed HTTP headers
5. **Thread Safety**: Guava's RateLimiter handles concurrent requests safely
6. **Enhanced Monitoring**: Tracks usage patterns and provides reset information

## Considerations

1. **Memory Usage**: Each unique client IP creates a RateLimiter instance
2. **IP Spoofing**: Rate limiting by IP can be bypassed with IP rotation
3. **Proxy Support**: Properly handles common proxy headers
4. **Rate Limit Scope**: Applied globally to all `/api/*` endpoints

## Future Enhancements

- Add configurable rate limits per endpoint
- Implement Redis-based distributed rate limiting
- Add user-based rate limiting (not just IP-based)
- Support for rate limit exemptions
- Metrics and monitoring integration