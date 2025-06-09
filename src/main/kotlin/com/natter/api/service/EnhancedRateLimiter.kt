package com.natter.api.service

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock

/**
 * Enhanced rate limiter that implements a sliding window approach
 * allowing exactly N requests per second window.
 */
class EnhancedRateLimiter(private val requestsPerSecond: Double) {
    
    private val requestTimestamps = ConcurrentLinkedQueue<Long>()
    private val windowStartTime = AtomicLong(Instant.now().toEpochMilli())
    private val lock = ReentrantLock()
    
    companion object {
        private const val WINDOW_SIZE_MS = 1000L // 1 second window
    }
    
    fun tryAcquire(): Boolean {
        lock.lock()
        try {
            val now = Instant.now().toEpochMilli()
            cleanupOldRequests(now)
            
            if (requestTimestamps.size < requestsPerSecond.toInt()) {
                requestTimestamps.offer(now)
                return true
            }
            return false
        } finally {
            lock.unlock()
        }
    }
    
    fun getRemainingRequests(): Int {
        lock.lock()
        try {
            val now = Instant.now().toEpochMilli()
            cleanupOldRequests(now)
            return (requestsPerSecond.toInt() - requestTimestamps.size).coerceAtLeast(0)
        } finally {
            lock.unlock()
        }
    }
    
    fun getResetTimeMillis(): Long {
        lock.lock()
        try {
            if (requestTimestamps.isEmpty()) {
                return System.currentTimeMillis() + WINDOW_SIZE_MS
            }
            val oldestRequest = requestTimestamps.peek() ?: return System.currentTimeMillis() + WINDOW_SIZE_MS
            return oldestRequest + WINDOW_SIZE_MS
        } finally {
            lock.unlock()
        }
    }
    
    fun getLimit(): Int {
        return requestsPerSecond.toInt()
    }
    
    private fun cleanupOldRequests(currentTime: Long) {
        val cutoffTime = currentTime - WINDOW_SIZE_MS
        while (requestTimestamps.isNotEmpty() && requestTimestamps.peek()!! < cutoffTime) {
            requestTimestamps.poll()
        }
    }
}