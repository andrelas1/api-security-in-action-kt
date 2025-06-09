package com.natter.api.demo

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Demonstration showing the difference between FilterRegistrationBean
 * and @Component annotation for filter registration in Spring Boot.
 */
object FilterRegistrationDemo {

    private val requestLog = ConcurrentHashMap<String, AtomicLong>()

    @JvmStatic
    fun main(args: Array<String>) {
        println("=== FilterRegistrationBean vs @Component Demonstration ===\n")
        
        demonstrateFilterBehavior()
        println("\n" + "=".repeat(60) + "\n")
        
        demonstrateUrlPatternMatching()
        println("\n" + "=".repeat(60) + "\n")
        
        demonstrateFilterOrdering()
    }

    /**
     * Shows how filters behave differently based on registration method
     */
    private fun demonstrateFilterBehavior() {
        println("1. Filter Registration Methods Comparison")
        println("Showing how different registration affects which URLs are filtered\n")

        // Simulate different types of HTTP requests
        val requests = listOf(
            "/api/spaces" to "API Request",
            "/api/users" to "API Request", 
            "/favicon.ico" to "Static Resource",
            "/css/styles.css" to "Static Resource",
            "/js/app.js" to "Static Resource",
            "/images/logo.png" to "Static Resource",
            "/health" to "Health Check",
            "/admin/dashboard" to "Admin Request"
        )

        println("--- With @Component (applies to ALL requests) ---")
        requests.forEach { (url, type) ->
            val shouldFilter = true // @Component applies to everything
            println("$url ($type): ${if (shouldFilter) "FILTERED" else "NOT FILTERED"}")
        }

        println("\n--- With FilterRegistrationBean urlPattern('/api/*') ---")
        requests.forEach { (url, type) ->
            val shouldFilter = url.startsWith("/api/")
            println("$url ($type): ${if (shouldFilter) "FILTERED" else "NOT FILTERED"}")
        }

        println("\nKey Difference:")
        println("- @Component: Rate limits EVERYTHING (inefficient)")
        println("- FilterRegistrationBean: Rate limits only /api/* (efficient)")
    }

    /**
     * Demonstrates URL pattern matching capabilities
     */
    private fun demonstrateUrlPatternMatching() {
        println("2. URL Pattern Matching Examples")
        println("How FilterRegistrationBean patterns match different URLs\n")

        val patterns = mapOf(
            "/api/*" to listOf("/api/spaces", "/api/users", "/api/auth/login"),
            "/admin/*" to listOf("/admin/dashboard", "/admin/users", "/admin/settings"),
            "*.json" to listOf("/data.json", "/config.json", "/api/spaces.json"),
            "/public/*" to listOf("/public/docs", "/public/images/logo.png"),
            "/*" to listOf("/anything", "/api/spaces", "/favicon.ico")
        )

        patterns.forEach { (pattern, urls) ->
            println("Pattern: '$pattern'")
            urls.forEach { url ->
                val matches = when {
                    pattern == "/*" -> true
                    pattern.endsWith("/*") -> url.startsWith(pattern.dropLast(1))
                    pattern.startsWith("*.") -> url.endsWith(pattern.drop(1))
                    else -> url == pattern
                }
                println("  $url: ${if (matches) "MATCHES" else "NO MATCH"}")
            }
            println()
        }
    }

    /**
     * Shows how filter ordering works with FilterRegistrationBean
     */
    private fun demonstrateFilterOrdering() {
        println("3. Filter Ordering with FilterRegistrationBean")
        println("How order property affects filter execution sequence\n")

        // Simulate filter chain execution
        val filters = listOf(
            "SecurityFilter" to 1,
            "RateLimitFilter" to 2,
            "LoggingFilter" to 3,
            "CorsFilter" to 0,  // Lowest order = executes first
            "AuthFilter" to 1
        ).sortedBy { it.second }

        println("Filter execution order (based on order property):")
        filters.forEachIndexed { index, (filterName, order) ->
            println("${index + 1}. $filterName (order=$order)")
        }

        println("\nRequest flow through filter chain:")
        println("HTTP Request")
        filters.forEach { (filterName, _) ->
            println("    ↓")
            println("$filterName.doFilter()")
        }
        println("    ↓")
        println("Controller")
        println("    ↓")
        println("HTTP Response")
        filters.reversed().forEach { (filterName, _) ->
            println("    ↓")
            println("$filterName (response processing)")
        }
    }

    /**
     * Example of what NOT to do - using @Component for rate limiting
     */
    @Component
    class BadRateLimitFilter : Filter {
        override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
            val httpRequest = request as HttpServletRequest
            val url = httpRequest.requestURI
            
            // This gets called for EVERY request!
            println("BAD: Rate limiting $url (even static resources!)")
            
            // Unnecessary CPU usage for favicon.ico, CSS, JS files, etc.
            performExpensiveRateLimitCheck(url)
            
            chain.doFilter(request, response)
        }
        
        private fun performExpensiveRateLimitCheck(url: String) {
            // Simulates the overhead of rate limit checking
            Thread.sleep(1) // Even 1ms per static resource adds up!
        }
    }

    /**
     * Example of proper FilterRegistrationBean configuration
     */
    @Configuration
    class GoodFilterConfig {
        
        @Bean
        fun rateLimitFilterRegistration(): FilterRegistrationBean<GoodRateLimitFilter> {
            val registration = FilterRegistrationBean<GoodRateLimitFilter>()
            registration.filter = GoodRateLimitFilter()
            registration.addUrlPatterns("/api/*")  // Only API endpoints
            registration.setName("rateLimitFilter")
            registration.order = 2  // After security, before logging
            return registration
        }
        
        @Bean
        fun loggingFilterRegistration(): FilterRegistrationBean<RequestLoggingFilter> {
            val registration = FilterRegistrationBean<RequestLoggingFilter>()
            registration.filter = RequestLoggingFilter()
            registration.addUrlPatterns("/api/*", "/admin/*")  // Multiple patterns
            registration.setName("loggingFilter")
            registration.order = 3  // After rate limiting
            return registration
        }
    }

    /**
     * Example of properly scoped rate limit filter
     */
    class GoodRateLimitFilter : Filter {
        override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
            val httpRequest = request as HttpServletRequest
            val url = httpRequest.requestURI
            
            // This only gets called for /api/* requests
            println("GOOD: Rate limiting $url (API endpoints only)")
            
            // CPU usage only where needed
            performRateLimitCheck(url)
            
            chain.doFilter(request, response)
        }
        
        private fun performRateLimitCheck(url: String) {
            // Rate limiting logic only for API endpoints
            val count = requestLog.computeIfAbsent(url) { AtomicLong(0) }
            count.incrementAndGet()
        }
    }

    /**
     * Example logging filter with specific URL patterns
     */
    class RequestLoggingFilter : Filter {
        override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
            val httpRequest = request as HttpServletRequest
            val startTime = System.currentTimeMillis()
            
            chain.doFilter(request, response)
            
            val duration = System.currentTimeMillis() - startTime
            println("Request ${httpRequest.requestURI} took ${duration}ms")
        }
    }
}

/*
 * KEY INSIGHTS FROM THIS DEMONSTRATION:
 * 
 * 1. SCOPE CONTROL:
 *    @Component: Applies to ALL requests (wasteful)
 *    FilterRegistrationBean: Applies only to specified patterns (efficient)
 * 
 * 2. PERFORMANCE IMPACT:
 *    Without URL patterns: Rate limiting favicon.ico, CSS, JS files
 *    With URL patterns: Rate limiting only API endpoints
 * 
 * 3. CONFIGURATION FLEXIBILITY:
 *    @Component: Limited configuration options
 *    FilterRegistrationBean: Full control over patterns, order, naming
 * 
 * 4. MAINTAINABILITY:
 *    @Component: Hard to modify scope without changing filter code
 *    FilterRegistrationBean: Easy to adjust patterns in configuration
 * 
 * 5. REAL-WORLD SCENARIOS:
 *    - Different rate limits for different API versions
 *    - Environment-specific filter configurations
 *    - Multiple filters with specific execution order
 *    - Conditional filter registration based on properties
 * 
 * CONCLUSION:
 * FilterRegistrationBean is essential for production applications where
 * you need precise control over which requests get filtered and in what order.
 */