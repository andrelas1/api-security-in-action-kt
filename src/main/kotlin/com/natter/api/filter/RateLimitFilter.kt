package com.natter.api.filter

import com.google.common.util.concurrent.RateLimiter
import com.natter.api.service.EnhancedRateLimiter
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter : Filter {

    private val rateLimiters = ConcurrentHashMap<String, EnhancedRateLimiter>()
    
    companion object {
        private const val REQUESTS_PER_SECOND = 5.0
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val clientId = getClientId(httpRequest)
        val rateLimiter = getRateLimiter(clientId)

        if (!rateLimiter.tryAcquire()) {
            handleRateLimitExceeded(httpResponse)
            return
        }

        // Add rate limit headers
        addRateLimitHeaders(httpResponse, rateLimiter)
        
        chain.doFilter(request, response)
    }

    private fun getRateLimiter(clientId: String): EnhancedRateLimiter {
        return rateLimiters.computeIfAbsent(clientId) {
            EnhancedRateLimiter(REQUESTS_PER_SECOND)
        }
    }

    private fun getClientId(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIP = request.getHeader("X-Real-IP")
        
        return when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIP.isNullOrBlank() -> xRealIP
            else -> request.remoteAddr ?: "unknown"
        }
    }

    private fun handleRateLimitExceeded(response: HttpServletResponse) {
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        
        response.setHeader("X-RateLimit-Limit", REQUESTS_PER_SECOND.toInt().toString())
        response.setHeader("X-RateLimit-Remaining", "0")
        response.setHeader("Retry-After", "1")

        val errorResponse = """
            {
                "error": "Rate limit exceeded",
                "message": "Too many requests. Maximum ${REQUESTS_PER_SECOND.toInt()} requests per second allowed.",
                "retryAfter": 1
            }
        """.trimIndent()

        response.writer.write(errorResponse)
        response.writer.flush()
    }

    private fun addRateLimitHeaders(response: HttpServletResponse, rateLimiter: EnhancedRateLimiter) {
        response.setHeader("X-RateLimit-Limit", rateLimiter.getLimit().toString())
        response.setHeader("X-RateLimit-Remaining", rateLimiter.getRemainingRequests().toString())
        response.setHeader("X-RateLimit-Reset", rateLimiter.getResetTimeMillis().toString())
    }
}