package com.natter.api.debug

import com.natter.api.service.EnhancedRateLimiter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("Rate Limiter Debug Test")
class RateLimiterDebugTest {

    @Test
    @DisplayName("debug rate limiter behavior")
    fun `debug rate limiter behavior`() {
        val rateLimiter = EnhancedRateLimiter(5.0)
        
        println("=== Initial State ===")
        println("Limit: ${rateLimiter.getLimit()}")
        println("Remaining: ${rateLimiter.getRemainingRequests()}")
        println("Reset Time: ${rateLimiter.getResetTimeMillis()}")
        
        println("\n=== Making 10 requests ===")
        repeat(10) { i ->
            val allowed = rateLimiter.tryAcquire()
            val remaining = rateLimiter.getRemainingRequests()
            println("Request ${i + 1}: allowed=$allowed, remaining=$remaining")
        }
        
        println("\n=== After waiting 1.1 seconds ===")
        Thread.sleep(1100)
        
        println("Remaining after wait: ${rateLimiter.getRemainingRequests()}")
        val allowedAfterWait = rateLimiter.tryAcquire()
        println("Request after wait: allowed=$allowedAfterWait")
        println("Remaining after new request: ${rateLimiter.getRemainingRequests()}")
    }
}