# Natter API Kotlin

This is an API build with Spring Boot and Kotlin. This is an implementation of the exercises in the Api Security in Action book.

To make things faster, I am "vibe coding" a lot in this project, usind the Zed text editor with its AI integration.

## Security measures applied so far

### Chapter 2
1. Request body validation (SpaceController.kt)
2. Content-Type header validation (SpaceController.kt)
3. Database user with proper granted permission (schema.sql)
4. Exception handler to not expose trace information in case of errors (GlobalExceptionHandler.kt)
5. Recommended security headers to all responses (ResponseHeaderAdvice.kt)

### Chapter 3
6. Rate Limiter on API level (RateLimiteFilter.kt)
