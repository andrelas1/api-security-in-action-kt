package com.natter.api.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import org.junit.jupiter.api.Assertions.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=never",
    "spring.jpa.hibernate.ddl-auto=none"
])
@DisplayName("Rate Limit Integration Tests")
class RateLimitIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private val restTemplate = TestRestTemplate()

    private fun getApiUrl(): String = "http://localhost:$port/api/spaces"

    @BeforeEach
    fun setUp() {
        // Wait a bit to ensure any previous rate limits have reset
        Thread.sleep(1100)
    }

    @Test
    @DisplayName("should apply rate limiting to API endpoints")
    fun `should apply rate limiting to API endpoints`() {
        val url = getApiUrl()
        val responses = mutableListOf<ResponseEntity<String>>()
        
        // Make rapid requests to trigger rate limiting
        repeat(10) {
            val response = restTemplate.getForEntity(url, String::class.java)
            responses.add(response)
        }
        
        // Verify we have rate limited responses (ignoring database errors)
        val rateLimitedResponses = responses.filter { it.statusCode == HttpStatus.TOO_MANY_REQUESTS }
        val nonRateLimitedResponses = responses.filter { it.statusCode != HttpStatus.TOO_MANY_REQUESTS }
        
        assertTrue(rateLimitedResponses.isNotEmpty(), "Should have some rate limited requests")
        assertTrue(nonRateLimitedResponses.isNotEmpty(), "Should have some non-rate-limited requests")
        
        // Check rate limit headers on blocked responses
        val rateLimitedResponse = rateLimitedResponses.first()
        assertEquals("5", rateLimitedResponse.headers["X-RateLimit-Limit"]?.first())
        assertEquals("0", rateLimitedResponse.headers["X-RateLimit-Remaining"]?.first())
        assertEquals("1", rateLimitedResponse.headers["Retry-After"]?.first())
        
        // Check error response body
        val responseBody = rateLimitedResponse.body
        assertNotNull(responseBody)
        assertTrue(responseBody!!.contains("Rate limit exceeded"))
        assertTrue(responseBody.contains("Too many requests"))
    }

    @Test
    @DisplayName("should block requests after limit exceeded")
    fun `should block requests after limit exceeded`() {
        val url = getApiUrl()
        
        // Make many requests to ensure we hit the limit
        var rateLimitedCount = 0
        repeat(10) {
            val response = restTemplate.getForEntity(url, String::class.java)
            if (response.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                rateLimitedCount++
            }
        }
        
        // Should have some rate limited responses
        assertTrue(rateLimitedCount > 0, "Should have rate limited responses when exceeding limit")
    }

    @Test
    @DisplayName("should reset rate limit after time window")
    fun `should reset rate limit after time window`() {
        val url = getApiUrl()
        
        // Exhaust rate limit
        repeat(6) {
            restTemplate.getForEntity(url, String::class.java)
        }
        
        // Wait for rate limit window to reset
        Thread.sleep(1100)
        
        // Make another request - should not be rate limited
        val response = restTemplate.getForEntity(url, String::class.java)
        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode, "Should not be rate limited after reset")
    }

    @Test
    @DisplayName("should apply rate limiting to POST requests")
    fun `should apply rate limiting to POST requests`() {
        val url = getApiUrl()
        val requestBody = """{"name": "Test Space", "owner": "testuser"}"""
        
        var rateLimitedCount = 0
        repeat(8) {
            val response = restTemplate.postForEntity(url, requestBody, String::class.java)
            if (response.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                rateLimitedCount++
            }
        }
        
        assertTrue(rateLimitedCount > 0, "POST requests should be rate limited")
    }

    @Test
    @DisplayName("should only apply to API endpoints")
    fun `should only apply to API endpoints`() {
        // API endpoint should be rate limited after many requests
        val apiUrl = getApiUrl()
        var apiRateLimited = false
        repeat(10) {
            val response = restTemplate.getForEntity(apiUrl, String::class.java)
            if (response.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
                apiRateLimited = true
            }
        }
        assertTrue(apiRateLimited, "API endpoints should be rate limited")
        
        // Non-API endpoints should not be rate limited (though they might return 404)
        val nonApiUrl = "http://localhost:$port/favicon.ico"
        repeat(10) {
            val response = restTemplate.getForEntity(nonApiUrl, String::class.java)
            // Should not return 429 even after many requests
            assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode)
        }
    }
}