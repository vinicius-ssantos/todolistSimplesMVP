package com.viniss.todo.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LoginAttemptServiceTest {

    private lateinit var service: LoginAttemptService

    @BeforeEach
    fun setUp() {
        service = LoginAttemptService()
    }

    @Test
    fun `should not block account with no failed attempts`() {
        assertFalse(service.isBlocked("test@example.com"))
    }

    @Test
    fun `should allow 4 failed attempts without blocking`() {
        val email = "test@example.com"

        repeat(4) {
            service.recordFailedAttempt(email)
        }

        assertFalse(service.isBlocked(email))
        assertEquals(1, service.getRemainingAttempts(email))
    }

    @Test
    fun `should block account after 5 failed attempts`() {
        val email = "test@example.com"

        repeat(5) {
            service.recordFailedAttempt(email)
        }

        assertTrue(service.isBlocked(email))
        assertEquals(0, service.getRemainingAttempts(email))
    }

    @Test
    fun `should reset failed attempts on success`() {
        val email = "test@example.com"

        repeat(3) {
            service.recordFailedAttempt(email)
        }

        service.resetFailedAttempts(email)

        assertFalse(service.isBlocked(email))
        assertEquals(5, service.getRemainingAttempts(email))
    }

    @Test
    fun `should track attempts for different emails separately`() {
        val email1 = "user1@example.com"
        val email2 = "user2@example.com"

        repeat(5) {
            service.recordFailedAttempt(email1)
        }

        repeat(2) {
            service.recordFailedAttempt(email2)
        }

        assertTrue(service.isBlocked(email1))
        assertFalse(service.isBlocked(email2))
        assertEquals(0, service.getRemainingAttempts(email1))
        assertEquals(3, service.getRemainingAttempts(email2))
    }

    @Test
    fun `should return correct remaining attempts`() {
        val email = "test@example.com"

        assertEquals(5, service.getRemainingAttempts(email))

        service.recordFailedAttempt(email)
        assertEquals(4, service.getRemainingAttempts(email))

        service.recordFailedAttempt(email)
        assertEquals(3, service.getRemainingAttempts(email))

        service.recordFailedAttempt(email)
        assertEquals(2, service.getRemainingAttempts(email))
    }
}
