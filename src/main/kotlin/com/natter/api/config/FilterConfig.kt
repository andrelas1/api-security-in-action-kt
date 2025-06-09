package com.natter.api.config

import com.natter.api.filter.RateLimitFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for servlet filters using FilterRegistrationBean.
 * 
 * FilterRegistrationBean provides programmatic control over filter registration,
 * allowing us to specify exactly which URLs should be filtered and in what order.
 * This is more efficient and flexible than using @Component which would apply
 * the filter to ALL requests (including static resources like CSS, JS, images).
 */
@Configuration
class FilterConfig {

    /**
     * Registers the RateLimitFilter with specific URL patterns and configuration.
     * 
     * Using FilterRegistrationBean instead of @Component because:
     * - Only applies to /api/* endpoints (not static resources)
     * - Provides control over filter execution order
     * - Allows naming the filter for debugging/monitoring
     * - More efficient than filtering every single HTTP request
     * 
     * Without this configuration, using @Component would mean:
     * - Rate limiting favicon.ico, CSS, JS files (waste of CPU)
     * - No control over when the filter executes relative to other filters
     * - Harder to conditionally enable/disable the filter
     */
    @Bean
    fun rateLimitFilterRegistration(rateLimitFilter: RateLimitFilter): FilterRegistrationBean<RateLimitFilter> {
        // Create a new FilterRegistrationBean to configure our rate limit filter
        val registration = FilterRegistrationBean<RateLimitFilter>()
        
        // Specify which filter instance to register (Spring will inject RateLimitFilter)
        registration.filter = rateLimitFilter
        
        // Only apply rate limiting to API endpoints, not static resources
        // This means /api/spaces gets rate limited, but /favicon.ico does not
        registration.addUrlPatterns("/api/*")
        
        // Give the filter a name for identification in logs and monitoring tools
        registration.setName("rateLimitFilter")
        
        // Set execution order (lower numbers execute first in the filter chain)
        // Order 1 means this runs early, before business logic filters
        registration.order = 1
        
        // Return the configured registration bean for Spring to manage
        return registration
    }
}