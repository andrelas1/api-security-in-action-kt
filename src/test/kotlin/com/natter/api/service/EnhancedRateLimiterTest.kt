package com.natter.api.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.*

@DisplayName("EnhancedRateLimiter")
class EnhancedRateLimiterTest {

    private lateinit var rateLimiter: EnhancedRateLimiter

    @BeforeEach
    fun setUp() {
        rateLimiter = EnhancedRateLimiter(5.0) // 5 requests per second
    }

    @Test
    @DisplayName("should allow requests within limit")
    fun `should allow requests within limit`() {
        // When - make requests within limit
        repeat(5) { requestNum ->
            val allowed = rateLimiter.tryAcquire()
            assertTrue(allowed, "Request ${requestNum + 1} should be allowed")
        }
    }

    @Test
    @DisplayName("should block requests exceeding limit")
    fun `should block requests exceeding limit`() {
        // Given - exhaust the rate limit
        repeat(5) { rateLimiter.tryAcquire() }
        
        // When - try additional requests
        val blocked1 = rateLimiter.tryAcquire()
        val blocked2 = rateLimiter.tryAcquire()
        
        // Then
        assertFalse(blocked1, "6th request should be blocked")
        assertFalse(blocked2, "7th request should be blocked")
    }

    @Test
    @DisplayName("should return correct initial remaining count")
    fun `should return correct initial remaining count`() {
        val remaining = rateLimiter.getRemainingRequests()
        assertEquals(5, remaining, "Should start with full quota")
    }

    @Test
    @DisplayName("should decrease remaining count as requests are made")
    fun `should decrease remaining count as requests are made`() {
        // Initial state
        assertEquals(5, rateLimiter.getRemainingRequests())
        
        // After 1 request
        rateLimiter.tryAcquire()
        assertEquals(4, rateLimiter.getRemainingRequests())
        
        // After 3 more requests
        repeat(3) { rateLimiter.tryAcquire() }
        assertEquals(1, rateLimiter.getRemainingRequests())
        
        // After exhausting quota
        rateLimiter.tryAcquire()
        assertEquals(0, rateLimiter.getRemainingRequests())
    }

    @Test
    @DisplayName("should return correct limit")
    fun `should return correct limit`() {
        assertEquals(5, rateLimiter.getLimit())
    }

    @Test
    @DisplayName("should provide reset time")
    fun `should provide reset time`() {
        val beforeRequest = System.currentTimeMillis()
        rateLimiter.tryAcquire()
        val afterRequest = System.currentTimeMillis()
        
        val resetTime = rateLimiter.getResetTimeMillis()
        
        // Reset time should be approximately 1 second after first request
        assertTrue(resetTime > beforeRequest + 900, "Reset time should be ~1 second after request")
        assertTrue(resetTime < afterRequest + 1100, "Reset time should be reasonable")
    }

    @Test
    @DisplayName("should reset after time window")
    fun `should reset after time window`() {
        // Given - exhaust rate limit
        repeat(5) { rateLimiter.tryAcquire() }
        assertFalse(rateLimiter.tryAcquire(), "Should be blocked initially")
        
        // When - wait for window reset
        Thread.sleep(1100)
        
        // Then - should allow requests again
        assertTrue(rateLimiter.tryAcquire(), "Should allow requests after reset")
        assertTrue(rateLimiter.tryAcquire(), "Should allow multiple requests after reset")
    }

    @Test
    @DisplayName("should work with different rate limits")
    fun `should work with different rate limits`() {
        val slowLimiter = EnhancedRateLimiter(2.0) // 2 requests per second
        assertEquals(2, slowLimiter.getLimit())
        
        // Should allow exactly 2 requests
        assertTrue(slowLimiter.tryAcquire())
        assertTrue(slowLimiter.tryAcquire())
        assertFalse(slowLimiter.tryAcquire())
    }

    @Test
    @DisplayName("should handle rapid successive requests correctly")
    fun `should handle rapid successive requests correctly`() {
        val results = mutableListOf<Boolean>()
        
        // Make 10 rapid requests
        repeat(10) {
            results.add(rateLimiter.tryAcquire())
        }
        
        val allowedCount = results.count { it }
        val blockedCount = results.count { !it }
        
        assertEquals(5, allowedCount, "Should allow exactly 5 requests")
        assertEquals(5, blockedCount, "Should block exactly 5 requests")
    }
}