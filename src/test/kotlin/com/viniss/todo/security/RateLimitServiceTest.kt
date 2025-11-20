package com.viniss.todo.security

import com.viniss.todo.config.RateLimitConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RateLimitServiceTest {

    private lateinit var service: RateLimitService

    @BeforeEach
    fun setUp() {
        val config = RateLimitConfig(
            maxRequests = 5,
            windowMinutes = 1,
            paths = listOf("/api/auth/"),
            enabled = true
        )
        service = RateLimitService(config)
    }

    @Test
    fun `should allow first 5 requests`() {
        val key = "test-ip:login"

        repeat(5) {
            assertTrue(service.isAllowed(key), "Request ${it + 1} should be allowed")
        }
    }

    @Test
    fun `should block 6th request`() {
        val key = "test-ip:login"

        // Consume 5 tokens
        repeat(5) {
            service.isAllowed(key)
        }

        // 6th request should be blocked
        assertFalse(service.isAllowed(key))
    }

    @Test
    fun `should track different keys separately`() {
        val key1 = "ip1:login"
        val key2 = "ip2:login"

        // Consume all tokens for key1
        repeat(5) {
            service.isAllowed(key1)
        }

        // key1 should be blocked
        assertFalse(service.isAllowed(key1))

        // key2 should still be allowed
        assertTrue(service.isAllowed(key2))
    }

    @Test
    fun `should create bucket for new key`() {
        val newKey = "new-ip:login"
        val bucket = service.resolveBucket(newKey)

        assertNotNull(bucket)
        assertTrue(service.isAllowed(newKey))
    }

    @Test
    fun `should reuse existing bucket for same key`() {
        val key = "test-ip:login"

        val bucket1 = service.resolveBucket(key)
        val bucket2 = service.resolveBucket(key)

        assertSame(bucket1, bucket2, "Should return the same bucket instance")
    }
}
