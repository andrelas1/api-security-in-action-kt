package com.natter.api.demo

import com.natter.api.service.EnhancedRateLimiter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * Demonstration showing why the rateLimiter parameter is now properly used
 * in the addRateLimitHeaders method after introducing EnhancedRateLimiter.
 */
object RateLimiterParameterDemo {

    @JvmStatic
    fun main(args: Array<String>) {
        println("=== RateLimiter Parameter Usage Demonstration ===\n")
        
        demonstrateParameterUsage()
        println("\n" + "=".repeat(60) + "\n")
        
        demonstrateHeaderValues()
        println("\n" + "=".repeat(60) + "\n")
        
        demonstrateRealTimeTracking()
    }

    /**
     * Shows how the rateLimiter parameter provides meaningful information
     */
    private fun demonstrateParameterUsage() {
        println("1. RateLimiter Parameter Usage")
        println("Before: Parameter was unused because Guava's RateLimiter doesn't expose usage info")
        println("After: EnhancedRateLimiter wrapper provides detailed information\n")

        val rateLimiter = EnhancedRateLimiter(5.0)
        
        println("Initial state:")
        printRateLimiterInfo(rateLimiter, "Fresh RateLimiter")
        
        // Make some requests
        println("\nAfter making 3 requests:")
        repeat(3) { 
            val allowed = rateLimiter.tryAcquire()
            println("Request ${it + 1}: ${if (allowed) "ALLOWED" else "BLOCKED"}")
        }
        printRateLimiterInfo(rateLimiter, "After 3 requests")
        
        // Try to exceed limit
        println("\nAttempting 3 more requests (should hit limit):")
        repeat(3) { 
            val allowed = rateLimiter.tryAcquire()
            println("Request ${it + 4}: ${if (allowed) "ALLOWED" else "BLOCKED"}")
        }
        printRateLimiterInfo(rateLimiter, "After 6 total requests")
    }

    /**
     * Shows how headers are now populated with real data from rateLimiter parameter
     */
    private fun demonstrateHeaderValues() {
        println("2. HTTP Header Values from RateLimiter Parameter")
        
        val rateLimiter = EnhancedRateLimiter(5.0)
        
        // Simulate different request scenarios
        val scenarios = listOf(
            "First request" to 0,
            "Third request" to 2,
            "At limit" to 5,
            "Over limit" to 7
        )
        
        scenarios.forEach { (scenario, requestCount) ->
            // Reset rate limiter for clean demo
            val freshRateLimiter = EnhancedRateLimiter(5.0)
            
            // Make the specified number of requests
            repeat(requestCount) { freshRateLimiter.tryAcquire() }
            
            // Show what headers would be set
            println("\n$scenario ($requestCount requests made):")
            simulateAddRateLimitHeaders(freshRateLimiter)
        }
    }

    /**
     * Shows real-time tracking across multiple clients
     */
    private fun demonstrateRealTimeTracking() {
        println("3. Real-time Tracking Across Multiple Clients")
        
        val rateLimiters = ConcurrentHashMap<String, EnhancedRateLimiter>()
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(20)
        
        // Simulate requests from different IPs
        val clientIPs = listOf("192.168.1.1", "192.168.1.2", "192.168.1.3")
        
        repeat(20) { requestNum ->
            executor.submit {
                try {
                    val clientIP = clientIPs[requestNum % clientIPs.size]
                    
                    val rateLimiter = rateLimiters.computeIfAbsent(clientIP) {
                        EnhancedRateLimiter(5.0)
                    }
                    
                    val allowed = rateLimiter.tryAcquire()
                    
                    synchronized(System.out) {
                        println("Request $requestNum from $clientIP: ${if (allowed) "ALLOWED" else "BLOCKED"}")
                        simulateAddRateLimitHeaders(rateLimiter, clientIP)
                        println()
                    }
                    
                } finally {
                    latch.countDown()
                }
            }
        }
        
        latch.await()
        executor.shutdown()
        
        println("Final state for all clients:")
        rateLimiters.forEach { (clientIP, rateLimiter) ->
            printRateLimiterInfo(rateLimiter, "Client $clientIP")
        }
    }

    /**
     * Helper method that simulates the addRateLimitHeaders method
     */
    private fun simulateAddRateLimitHeaders(rateLimiter: EnhancedRateLimiter, clientId: String = "client") {
        // This simulates what happens in the actual filter method:
        // private fun addRateLimitHeaders(response: HttpServletResponse, rateLimiter: EnhancedRateLimiter)
        
        val limit = rateLimiter.getLimit()
        val remaining = rateLimiter.getRemainingRequests()
        val resetTime = rateLimiter.getResetTimeMillis()
        
        println("  Headers for $clientId:")
        println("    X-RateLimit-Limit: $limit")
        println("    X-RateLimit-Remaining: $remaining")
        println("    X-RateLimit-Reset: $resetTime")
    }

    /**
     * Helper method to print rateLimiter information
     */
    private fun printRateLimiterInfo(rateLimiter: EnhancedRateLimiter, label: String) {
        println("$label:")
        println("  - Limit: ${rateLimiter.getLimit()}")
        println("  - Remaining: ${rateLimiter.getRemainingRequests()}")
        println("  - Reset Time: ${rateLimiter.getResetTimeMillis()}")
    }
}

/*
 * KEY INSIGHTS ABOUT THE PARAMETER USAGE:
 * 
 * BEFORE (With Guava RateLimiter):
 * ===============================
 * private fun addRateLimitHeaders(response: HttpServletResponse, rateLimiter: RateLimiter) {
 *     // rateLimiter parameter was UNUSED because:
 *     // - Guava's RateLimiter doesn't expose remaining permits
 *     // - Can't query current usage statistics
 *     // - No way to get reset time information
 *     
 *     response.setHeader("X-RateLimit-Limit", "5") // Hard-coded!
 *     response.setHeader("X-RateLimit-Applied", "true") // Generic info
 * }
 * 
 * AFTER (With EnhancedRateLimiter):
 * ================================
 * private fun addRateLimitHeaders(response: HttpServletResponse, rateLimiter: EnhancedRateLimiter) {
 *     // rateLimiter parameter is NOW USED because:
 *     // - EnhancedRateLimiter tracks remaining requests
 *     // - Provides reset time information
 *     // - Gives real-time usage statistics
 *     
 *     response.setHeader("X-RateLimit-Limit", rateLimiter.getLimit().toString())
 *     response.setHeader("X-RateLimit-Remaining", rateLimiter.getRemainingRequests().toString())
 *     response.setHeader("X-RateLimit-Reset", rateLimiter.getResetTimeMillis().toString())
 * }
 * 
 * WHY THIS MATTERS:
 * ================
 * 1. MEANINGFUL HEADERS: Clients get real-time rate limit information
 * 2. PARAMETER JUSTIFICATION: The parameter now serves a clear purpose
 * 3. BETTER UX: Clients know exactly how many requests they have left
 * 4. DEBUGGING: Easier to troubleshoot rate limiting issues
 * 5. STANDARDS COMPLIANCE: Follows HTTP rate limiting header conventions
 * 
 * THE LESSON:
 * ==========
 * Sometimes unused parameters indicate:
 * - Incomplete implementation
 * - Missing functionality in dependencies
 * - Opportunity for enhancement
 * 
 * In this case, wrapping Guava's RateLimiter with additional tracking
 * transformed an unused parameter into a valuable source of information!
 */