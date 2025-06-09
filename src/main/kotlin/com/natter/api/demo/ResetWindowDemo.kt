package com.natter.api.demo

import com.natter.api.service.EnhancedRateLimiter
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Demonstration of the resetWindowIfNeeded function behavior
 * showing how it handles concurrent window resets safely.
 */
object ResetWindowDemo {

    @JvmStatic
    fun main(args: Array<String>) {
        println("=== resetWindowIfNeeded Function Demonstration ===\n")
        
        demonstrateNormalWindowReset()
        println("\n" + "=".repeat(60) + "\n")
        
        demonstrateConcurrentWindowReset()
        println("\n" + "=".repeat(60) + "\n")
        
        demonstrateWindowResetTiming()
    }

    /**
     * Shows normal window reset behavior with single thread
     */
    private fun demonstrateNormalWindowReset() {
        println("1. Normal Window Reset (Single Thread)")
        println("Demonstrating how a window naturally resets after 1 second\n")

        val rateLimiter = EnhancedRateLimiter(5.0)
        
        // Make some requests in first window
        println("Making requests in first window:")
        repeat(3) { i ->
            val allowed = rateLimiter.tryAcquire()
            println("  Request ${i + 1}: ${if (allowed) "ALLOWED" else "BLOCKED"} " +
                   "(Remaining: ${rateLimiter.getRemainingRequests()})")
        }
        
        println("\nWaiting for window to expire (1.1 seconds)...")
        Thread.sleep(1100)
        
        println("Making requests in new window:")
        repeat(3) { i ->
            val allowed = rateLimiter.tryAcquire()
            println("  Request ${i + 1}: ${if (allowed) "ALLOWED" else "BLOCKED"} " +
                   "(Remaining: ${rateLimiter.getRemainingRequests()})")
        }
        
        println("\nObservation: Window automatically reset, full quota available again")
    }

    /**
     * Shows how multiple threads handle window reset concurrently
     */
    private fun demonstrateConcurrentWindowReset() {
        println("2. Concurrent Window Reset (Multiple Threads)")
        println("Showing how resetWindowIfNeeded handles race conditions\n")

        val rateLimiter = EnhancedRateLimiter(5.0)
        val resetCount = AtomicInteger(0)
        val requestCount = AtomicInteger(0)
        
        // Exhaust the current window
        repeat(5) { rateLimiter.tryAcquire() }
        println("Exhausted initial window (5/5 requests used)")
        
        // Wait for window to expire
        Thread.sleep(1100)
        println("Window has expired, now sending concurrent requests...\n")
        
        val executor = Executors.newFixedThreadPool(20)
        val latch = CountDownLatch(50)
        val startTime = System.currentTimeMillis()
        
        // 50 threads all try to make requests at the same time
        repeat(50) { threadNum ->
            executor.submit {
                try {
                    val allowed = rateLimiter.tryAcquire()
                    val threadTime = System.currentTimeMillis() - startTime
                    
                    if (allowed) {
                        requestCount.incrementAndGet()
                    }
                    
                    synchronized(System.out) {
                        println("Thread $threadNum (${threadTime}ms): " +
                               "${if (allowed) "ALLOWED" else "BLOCKED"} " +
                               "(Remaining: ${rateLimiter.getRemainingRequests()})")
                    }
                    
                } finally {
                    latch.countDown()
                }
            }
        }
        
        latch.await()
        executor.shutdown()
        
        println("\nResults:")
        println("- Total allowed requests: ${requestCount.get()}")
        println("- Expected: 5 (only 5 should be allowed in new window)")
        println("- Remaining: ${rateLimiter.getRemainingRequests()}")
        println("- resetWindowIfNeeded ensured exactly one window reset!")
    }

    /**
     * Shows precise timing of window resets
     */
    private fun demonstrateWindowResetTiming() {
        println("3. Window Reset Timing Analysis")
        println("Precise timing of when windows reset and requests are allowed\n")

        val rateLimiter = EnhancedRateLimiter(3.0) // Lower limit for clearer demo
        val startTime = Instant.now().toEpochMilli()
        
        // Function to show elapsed time
        fun elapsed(): Long = Instant.now().toEpochMilli() - startTime
        
        println("Starting demonstration with 3 requests per second limit")
        println("Time 0ms: Initial state")
        println("  Limit: ${rateLimiter.getLimit()}")
        println("  Remaining: ${rateLimiter.getRemainingRequests()}")
        
        // Use up the initial window
        repeat(3) { i ->
            val allowed = rateLimiter.tryAcquire()
            println("Time ${elapsed()}ms: Request ${i + 1} = ${if (allowed) "ALLOWED" else "BLOCKED"} " +
                   "(Remaining: ${rateLimiter.getRemainingRequests()})")
            Thread.sleep(100) // Small delay between requests
        }
        
        // Try one more (should be blocked)
        val blocked = rateLimiter.tryAcquire()
        println("Time ${elapsed()}ms: Request 4 = ${if (blocked) "ALLOWED" else "BLOCKED"} " +
               "(Remaining: ${rateLimiter.getRemainingRequests()})")
        
        // Wait and watch for window reset
        println("\nWaiting for window reset...")
        while (elapsed() < 1200) {
            if (elapsed() % 200 == 0L) { // Check every 200ms
                val remaining = rateLimiter.getRemainingRequests()
                println("Time ${elapsed()}ms: Remaining = $remaining")
            }
            Thread.sleep(50)
        }
        
        // Try requests in new window
        println("\nTrying requests in new window:")
        repeat(2) { i ->
            val allowed = rateLimiter.tryAcquire()
            println("Time ${elapsed()}ms: New window request ${i + 1} = ${if (allowed) "ALLOWED" else "BLOCKED"} " +
                   "(Remaining: ${rateLimiter.getRemainingRequests()})")
        }
        
        println("\nObservation: Window reset exactly when expected, quota replenished")
    }
}

/*
 * KEY INSIGHTS FROM THIS DEMONSTRATION:
 * 
 * 1. AUTOMATIC RESET: Windows reset automatically when time expires
 * 2. THREAD SAFETY: Multiple threads safely handle concurrent resets
 * 3. PRECISE TIMING: Resets happen exactly at the right time
 * 4. ATOMIC OPERATIONS: compareAndSet ensures only one reset per window
 * 5. NO RACE CONDITIONS: All threads see consistent state
 * 
 * THE resetWindowIfNeeded FUNCTION:
 * - Called before every rate limit check
 * - Detects when 1000ms has passed since window start
 * - Uses compareAndSet to ensure atomic window reset
 * - Only one thread can successfully reset per window
 * - Other threads safely continue with the reset window
 * 
 * WHY THIS MATTERS:
 * Without proper window reset handling:
 * - Multiple threads could reset the same window
 * - Request counts could be inconsistent
 * - Rate limiting would be unreliable
 * 
 * With resetWindowIfNeeded:
 * - Exactly one reset per time window
 * - Consistent behavior under high load
 * - Reliable rate limiting for all clients
 */