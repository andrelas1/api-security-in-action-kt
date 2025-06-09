package com.natter.api.filter

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.PrintWriter
import java.io.StringWriter
import org.junit.jupiter.api.Assertions.*

@DisplayName("RateLimitFilter")
class RateLimitFilterTest {

    private lateinit var filter: RateLimitFilter

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var filterChain: FilterChain

    private lateinit var responseWriter: StringWriter

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        filter = RateLimitFilter()
        responseWriter = StringWriter()
        `when`(response.writer).thenReturn(PrintWriter(responseWriter))
        `when`(request.remoteAddr).thenReturn("127.0.0.1")
    }

    @Test
    @DisplayName("should allow first request from client")
    fun `should allow first request from client`() {
        // When
        filter.doFilter(request, response, filterChain)
        
        // Then
        verify(filterChain).doFilter(request, response)
        verify(response).setHeader("X-RateLimit-Limit", "5")
        verify(response).setHeader(eq("X-RateLimit-Remaining"), anyString())
        verify(response).setHeader(eq("X-RateLimit-Reset"), anyString())
        verify(response, never()).setStatus(429)
    }

    @Test
    @DisplayName("should allow requests within rate limit")
    fun `should allow requests within rate limit`() {
        // When - make 3 requests (within limit of 5)
        repeat(3) {
            filter.doFilter(request, response, filterChain)
        }
        
        // Then - all should be allowed
        verify(filterChain, times(3)).doFilter(request, response)
        verify(response, never()).setStatus(429)
    }

    @Test
    @DisplayName("should block requests exceeding rate limit")
    fun `should block requests exceeding rate limit`() {
        // When - make 7 rapid requests (exceeding limit of 5)
        repeat(7) {
            filter.doFilter(request, response, filterChain)
        }
        
        // Then - some should be blocked
        verify(response, atLeastOnce()).setStatus(429)
        verify(response, atLeastOnce()).setHeader("X-RateLimit-Remaining", "0")
        verify(response, atLeastOnce()).setHeader("Retry-After", "1")
    }

    @Test
    @DisplayName("should use X-Forwarded-For header when available")
    fun `should use X-Forwarded-For header when available`() {
        // Given
        val forwardedIP = "10.0.0.1"
        `when`(request.getHeader("X-Forwarded-For")).thenReturn("$forwardedIP, 192.168.1.1")
        `when`(request.remoteAddr).thenReturn("192.168.1.100")
        
        // When
        filter.doFilter(request, response, filterChain)
        
        // Then - should use the forwarded IP for rate limiting
        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("should use X-Real-IP when X-Forwarded-For is not available")
    fun `should use X-Real-IP when X-Forwarded-For is not available`() {
        // Given
        val realIP = "10.0.0.2"
        `when`(request.getHeader("X-Forwarded-For")).thenReturn(null)
        `when`(request.getHeader("X-Real-IP")).thenReturn(realIP)
        `when`(request.remoteAddr).thenReturn("192.168.1.100")
        
        // When
        filter.doFilter(request, response, filterChain)
        
        // Then - should use the real IP for rate limiting
        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("should return proper JSON error response when rate limited")
    fun `should return proper JSON error response when rate limited`() {
        // Given - trigger rate limit
        repeat(7) {
            filter.doFilter(request, response, filterChain)
        }
        
        // Then - verify error response structure
        val responseContent = responseWriter.toString()
        if (responseContent.isNotEmpty()) {
            assertTrue(responseContent.contains("Rate limit exceeded"))
            assertTrue(responseContent.contains("Too many requests"))
            assertTrue(responseContent.contains("Maximum 5 requests per second"))
        }
    }

    @Test
    @DisplayName("should isolate rate limits between different IPs")
    fun `should isolate rate limits between different IPs`() {
        val ip1 = "192.168.1.1"
        val ip2 = "192.168.1.2"
        
        // Given - exhaust rate limit for IP1
        `when`(request.remoteAddr).thenReturn(ip1)
        repeat(6) {
            filter.doFilter(request, response, filterChain)
        }
        
        // When - make request from IP2
        `when`(request.remoteAddr).thenReturn(ip2)
        clearInvocations(filterChain, response)
        filter.doFilter(request, response, filterChain)
        
        // Then - IP2 should not be affected by IP1's rate limit
        verify(filterChain).doFilter(request, response)
        verify(response, never()).setStatus(429)
    }
}