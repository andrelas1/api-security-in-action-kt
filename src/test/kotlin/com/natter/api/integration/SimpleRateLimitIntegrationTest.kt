package com.natter.api.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.junit.jupiter.api.Assertions.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "spring.datasource.url=jdbc:h2:mem:simpletest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.sql.init.mode=never"
])
@DisplayName("Simple Rate Limit Tests")
class SimpleRateLimitIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private val restTemplate = TestRestTemplate()

    @Test
    @DisplayName("should return 429 when rate limit exceeded")
    fun `should return 429 when rate limit exceeded`() {
        val url = "http://localhost:$port/api/spaces"
        
        // Make rapid requests - some should be rate limited
        var rateLimitedCount = 0
        var successCount = 0
        
        repeat(10) {
            val response = restTemplate.getForEntity(url, String::class.java)
            when (response.statusCode) {
                HttpStatus.TOO_MANY_REQUESTS -> rateLimitedCount++
                else -> successCount++
            }
        }
        
        // We should have some rate limited responses
        assertTrue(rateLimitedCount > 0, "Should have some rate limited responses")
        
        // Verify rate limit headers are present on 429 responses
        val response = restTemplate.getForEntity(url, String::class.java)
        if (response.statusCode == HttpStatus.TOO_MANY_REQUESTS) {
            assertNotNull(response.headers["X-RateLimit-Limit"])
            assertNotNull(response.headers["Retry-After"])
        }
    }

    @Test
    @DisplayName("should not rate limit non-API endpoints")
    fun `should not rate limit non-API endpoints`() {
        val nonApiUrl = "http://localhost:$port/actuator/health"
        
        // Make many requests to non-API endpoint
        repeat(10) {
            val response = restTemplate.getForEntity(nonApiUrl, String::class.java)
            // Should never return 429, even if endpoint doesn't exist (404 is ok)
            assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode)
        }
    }
}