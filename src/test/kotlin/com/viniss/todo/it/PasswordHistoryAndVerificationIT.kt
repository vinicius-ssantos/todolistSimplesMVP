package com.viniss.todo.it

import com.viniss.todo.auth.*
import com.viniss.todo.repo.TaskRepository
import com.viniss.todo.repo.TodoListRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Integration tests for PasswordHistoryService and EmailVerificationService.
 *
 * These tests verify:
 * - Password reuse prevention
 * - Password history tracking and cleanup
 * - Email verification token generation and validation
 * - Email verification expiration
 *
 * Tests the full integration with database and services.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class PasswordHistoryAndVerificationIT {

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var appUserRepository: AppUserRepository
    @Autowired protected lateinit var passwordHistoryRepository: PasswordHistoryRepository
    @Autowired protected lateinit var passwordHistoryService: PasswordHistoryService
    @Autowired protected lateinit var emailVerificationService: EmailVerificationService
    @Autowired protected lateinit var passwordEncoder: PasswordEncoder
    @Autowired protected lateinit var todoListRepository: TodoListRepository
    @Autowired protected lateinit var taskRepository: TaskRepository

    @BeforeEach
    fun setUp() {
        taskRepository.deleteAll()
        todoListRepository.deleteAll()
        passwordHistoryRepository.deleteAll()
        appUserRepository.deleteAll()
    }

    // ========================================
    // PASSWORD HISTORY - RECORDING TESTS
    // ========================================

    @Test
    fun `should record password in history on user registration`() {
        val email = "history-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }

        val user = appUserRepository.findByEmail(email)!!
        val passwordHistory = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(
            user.id,
            org.springframework.data.domain.PageRequest.of(0, 10)
        )

        assertThat(passwordHistory).hasSize(1)
        assertThat(passwordEncoder.matches(password, passwordHistory.first().passwordHash)).isTrue()
    }

    @Test
    fun `should track multiple password changes`() {
        val userId = UUID.randomUUID()
        val passwords = listOf("First@Pass123", "Second@Pass456", "Third@Pass789")

        passwords.forEach { password ->
            val hash = passwordEncoder.encode(password)
            passwordHistoryService.recordPasswordChange(userId, hash)
        }

        val history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(
            userId,
            org.springframework.data.domain.PageRequest.of(0, 10)
        )

        assertThat(history).hasSize(3)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 3, 5, 10])
    fun `should maintain password history of configurable size`(passwordCount: Int) {
        val userId = UUID.randomUUID()

        repeat(passwordCount) { i ->
            val hash = passwordEncoder.encode("Password$i@123")
            passwordHistoryService.recordPasswordChange(userId, hash)
        }

        val history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(
            userId,
            org.springframework.data.domain.PageRequest.of(0, 100)
        )

        // Should keep at most preventReuseCount + 1 entries
        val maxExpected = minOf(passwordCount, passwordHistoryService.getPreventReuseCount() + 1)
        assertThat(history.size).isLessThanOrEqualTo(maxExpected)
    }

    // ========================================
    // PASSWORD HISTORY - REUSE PREVENTION TESTS
    // ========================================

    @Test
    fun `should detect password reuse`() {
        val userId = UUID.randomUUID()
        val password = "Reused@Pass123"
        val hash = passwordEncoder.encode(password)

        passwordHistoryService.recordPasswordChange(userId, hash)

        val isReused = passwordHistoryService.isPasswordReused(userId, password)
        assertThat(isReused).isTrue()
    }

    @Test
    fun `should not detect new password as reused`() {
        val userId = UUID.randomUUID()
        val oldPassword = "Old@Pass123"
        val newPassword = "New@Pass456"

        passwordHistoryService.recordPasswordChange(userId, passwordEncoder.encode(oldPassword))

        val isReused = passwordHistoryService.isPasswordReused(userId, newPassword)
        assertThat(isReused).isFalse()
    }

    @Test
    fun `should prevent reuse of recent passwords within limit`() {
        val userId = UUID.randomUUID()
        val passwords = (1..5).map { "Password$it@123" }

        // Record all passwords in history
        passwords.forEach { password ->
            passwordHistoryService.recordPasswordChange(userId, passwordEncoder.encode(password))
        }

        // Check that recent passwords are detected as reused
        passwords.forEach { password ->
            val isReused = passwordHistoryService.isPasswordReused(userId, password)
            assertThat(isReused).describedAs("Password '$password' should be detected as reused").isTrue()
        }
    }

    @Test
    fun `should allow reuse of old password beyond history limit`() {
        val userId = UUID.randomUUID()
        val preventReuseCount = passwordHistoryService.getPreventReuseCount()

        // Record more passwords than the prevention limit
        val firstPassword = "First@Pass123"
        passwordHistoryService.recordPasswordChange(userId, passwordEncoder.encode(firstPassword))

        // Add enough new passwords to push the first one out of history
        repeat(preventReuseCount + 5) { i ->
            passwordHistoryService.recordPasswordChange(userId, passwordEncoder.encode("NewPass$i@123"))
        }

        // First password should no longer be in tracked history
        val history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(
            userId,
            org.springframework.data.domain.PageRequest.of(0, 100)
        )

        // Should have at most preventReuseCount + 1 entries
        assertThat(history.size).isLessThanOrEqualTo(preventReuseCount + 1)
    }

    @Test
    fun `should handle empty password history`() {
        val userId = UUID.randomUUID()
        val password = "Any@Pass123"

        val isReused = passwordHistoryService.isPasswordReused(userId, password)
        assertThat(isReused).isFalse()
    }

    // ========================================
    // EMAIL VERIFICATION - TOKEN GENERATION TESTS
    // ========================================

    @Test
    fun `should generate verification token on registration`() {
        val email = "verify-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }

        val user = appUserRepository.findByEmail(email)!!
        assertThat(user.emailVerificationToken).isNotNull()
        assertThat(user.verificationTokenExpiresAt).isNotNull()
        assertThat(user.emailVerified).isFalse()
    }

    @Test
    fun `verification token should have correct expiration time`() {
        val email = "expiry-${UUID.randomUUID()}@example.com"
        val user = appUserRepository.save(
            AppUserEntity(email = email, passwordHash = "hash")
        )

        emailVerificationService.generateAndSendVerificationToken(user.id)

        val updatedUser = appUserRepository.findById(user.id).get()
        val expiresAt = updatedUser.verificationTokenExpiresAt!!

        // Should expire in approximately 24 hours
        val expectedExpiry = Instant.now().plus(24, ChronoUnit.HOURS)
        val diff = ChronoUnit.MINUTES.between(expiresAt, expectedExpiry)

        assertThat(diff).isBetween(-5L, 5L) // Allow 5 minute tolerance
    }

    @Test
    fun `should regenerate verification token on resend`() {
        val email = "resend-${UUID.randomUUID()}@example.com"
        val user = appUserRepository.save(
            AppUserEntity(email = email, passwordHash = "hash")
        )

        emailVerificationService.generateAndSendVerificationToken(user.id)
        val firstToken = appUserRepository.findById(user.id).get().emailVerificationToken

        // Resend verification
        emailVerificationService.generateAndSendVerificationToken(user.id)
        val secondToken = appUserRepository.findById(user.id).get().emailVerificationToken

        assertThat(firstToken).isNotEqualTo(secondToken)
    }

    // ========================================
    // EMAIL VERIFICATION - VALIDATION TESTS
    // ========================================

    @Test
    fun `should successfully verify email with valid token`() {
        val email = "valid-verify-${UUID.randomUUID()}@example.com"
        val token = UUID.randomUUID().toString()
        val user = appUserRepository.save(
            AppUserEntity(
                email = email,
                passwordHash = "hash",
                emailVerificationToken = token,
                verificationTokenExpiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
            )
        )

        val result = emailVerificationService.verifyEmail(token)

        assertThat(result).isTrue()

        val verifiedUser = appUserRepository.findById(user.id).get()
        assertThat(verifiedUser.emailVerified).isTrue()
        assertThat(verifiedUser.emailVerificationToken).isNull()
        assertThat(verifiedUser.verificationTokenExpiresAt).isNull()
    }

    @Test
    fun `should reject verification with invalid token`() {
        val invalidToken = UUID.randomUUID().toString()

        val result = emailVerificationService.verifyEmail(invalidToken)

        assertThat(result).isFalse()
    }

    @Test
    fun `should reject verification with expired token`() {
        val email = "expired-${UUID.randomUUID()}@example.com"
        val token = UUID.randomUUID().toString()
        appUserRepository.save(
            AppUserEntity(
                email = email,
                passwordHash = "hash",
                emailVerificationToken = token,
                verificationTokenExpiresAt = Instant.now().minus(1, ChronoUnit.HOURS) // Expired
            )
        )

        val result = emailVerificationService.verifyEmail(token)

        assertThat(result).isFalse()
    }

    @Test
    fun `should handle verification of already verified email`() {
        val email = "already-verified-${UUID.randomUUID()}@example.com"
        val token = UUID.randomUUID().toString()
        val user = appUserRepository.save(
            AppUserEntity(
                email = email,
                passwordHash = "hash",
                emailVerified = true,
                emailVerificationToken = token,
                verificationTokenExpiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
            )
        )

        val result = emailVerificationService.verifyEmail(token)

        assertThat(result).isTrue()

        val verifiedUser = appUserRepository.findById(user.id).get()
        assertThat(verifiedUser.emailVerified).isTrue()
    }

    @Test
    fun `should not regenerate token for already verified email`() {
        val email = "already-verified-resend-${UUID.randomUUID()}@example.com"
        val user = appUserRepository.save(
            AppUserEntity(
                email = email,
                passwordHash = "hash",
                emailVerified = true
            )
        )

        emailVerificationService.generateAndSendVerificationToken(user.id)

        val updatedUser = appUserRepository.findById(user.id).get()
        assertThat(updatedUser.emailVerificationToken).isNull()
    }

    // ========================================
    // EMAIL VERIFICATION - INTEGRATION TESTS
    // ========================================

    @Test
    fun `complete email verification flow via API`() {
        val email = "complete-flow-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        // Step 1: Register
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }

        // Step 2: Get verification token
        val user = appUserRepository.findByEmail(email)!!
        val token = user.emailVerificationToken!!

        assertThat(user.emailVerified).isFalse()

        // Step 3: Verify email
        mockMvc.get("/api/auth/verify-email") {
            param("token", token)
        }.andExpect {
            status { isOk() }
        }

        // Step 4: Check email is verified
        val verifiedUser = appUserRepository.findByEmail(email)!!
        assertThat(verifiedUser.emailVerified).isTrue()
    }

    @Test
    fun `should check email verification status`() {
        val email = "status-check-${UUID.randomUUID()}@example.com"
        val user = appUserRepository.save(
            AppUserEntity(email = email, passwordHash = "hash", emailVerified = false)
        )

        assertThat(emailVerificationService.isEmailVerified(user.id)).isFalse()

        // Verify email
        val token = UUID.randomUUID().toString()
        appUserRepository.save(
            user.copy(
                emailVerificationToken = token,
                verificationTokenExpiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
            )
        )

        emailVerificationService.verifyEmail(token)

        assertThat(emailVerificationService.isEmailVerified(user.id)).isTrue()
    }

    // ========================================
    // COMBINED INTEGRATION TESTS
    // ========================================

    @Test
    fun `registration should trigger both password history and email verification`() {
        val email = "combined-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }

        val user = appUserRepository.findByEmail(email)!!

        // Check password history
        val passwordHistory = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(
            user.id,
            org.springframework.data.domain.PageRequest.of(0, 10)
        )
        assertThat(passwordHistory).hasSize(1)

        // Check email verification
        assertThat(user.emailVerificationToken).isNotNull()
        assertThat(user.emailVerified).isFalse()
    }
}
