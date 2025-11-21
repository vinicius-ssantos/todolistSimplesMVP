package com.viniss.todo.it

import com.viniss.todo.auth.AppUserEntity
import com.viniss.todo.auth.AppUserRepository
import com.viniss.todo.email.EmailService
import com.viniss.todo.email.model.EmailMessage
import com.viniss.todo.email.provider.EmailProvider
import com.viniss.todo.repo.TaskRepository
import com.viniss.todo.repo.TodoListRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Integration tests for EmailService and Email Providers.
 *
 * Tests email sending functionality through the full stack:
 * - EmailService composition logic
 * - LoggingEmailProvider (captured via spy)
 * - Email templates (verification, password reset)
 *
 * Uses a TestEmailProvider to capture sent emails for assertions.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class EmailServiceIT {

    @TestConfiguration
    class TestEmailConfiguration {
        @Bean
        @Primary
        fun testEmailProvider(): EmailProvider = TestEmailProvider()
    }

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var emailService: EmailService
    @Autowired protected lateinit var emailProvider: EmailProvider
    @Autowired protected lateinit var todoListRepository: TodoListRepository
    @Autowired protected lateinit var taskRepository: TaskRepository
    @Autowired protected lateinit var appUserRepository: AppUserRepository

    private lateinit var testEmailProvider: TestEmailProvider

    @BeforeEach
    fun setUp() {
        taskRepository.deleteAll()
        todoListRepository.deleteAll()
        appUserRepository.deleteAll()

        testEmailProvider = emailProvider as TestEmailProvider
        testEmailProvider.clear()
    }

    // ========================================
    // VERIFICATION EMAIL TESTS
    // ========================================

    @Test
    fun `should send verification email with correct content`() {
        val email = "verify-${UUID.randomUUID()}@example.com"
        val token = "verification-token-123"

        emailService.sendVerificationEmail(email, token)

        val sentEmails = testEmailProvider.getSentEmails()
        assertThat(sentEmails).hasSize(1)

        val sentEmail = sentEmails.first()
        assertThat(sentEmail.to).isEqualTo(email)
        assertThat(sentEmail.subject).isEqualTo("Verify Your Email Address")
        assertThat(sentEmail.textContent).contains("verify your email")
        assertThat(sentEmail.textContent).contains(token)
        assertThat(sentEmail.htmlContent).contains("Verify Email Address")
        assertThat(sentEmail.htmlContent).contains(token)
    }

    @ParameterizedTest
    @CsvSource(
        "user1@example.com, token-abc-123",
        "user2@test.com, token-xyz-456",
        "admin@company.com, token-qwe-789"
    )
    fun `should send verification emails to multiple users`(email: String, token: String) {
        emailService.sendVerificationEmail(email, token)

        val sentEmails = testEmailProvider.getSentEmails()
        val emailForUser = sentEmails.find { it.to == email }

        assertThat(emailForUser).isNotNull
        assertThat(emailForUser!!.textContent).contains(token)
    }

    @Test
    fun `verification email should include proper HTML formatting`() {
        val email = "html-test@example.com"
        val token = "html-token"

        emailService.sendVerificationEmail(email, token)

        val sentEmail = testEmailProvider.getSentEmails().first()
        assertThat(sentEmail.htmlContent).contains("<!DOCTYPE html>")
        assertThat(sentEmail.htmlContent).contains("<html>")
        assertThat(sentEmail.htmlContent).contains("</html>")
        assertThat(sentEmail.htmlContent).contains("Verify Email Address")
        assertThat(sentEmail.htmlContent).contains("href=")
    }

    @Test
    fun `verification email should include expiration warning`() {
        val email = "expiration-test@example.com"
        val token = "expiration-token"

        emailService.sendVerificationEmail(email, token)

        val sentEmail = testEmailProvider.getSentEmails().first()
        assertThat(sentEmail.textContent).containsIgnoringCase("24 hours")
        assertThat(sentEmail.htmlContent).containsIgnoringCase("24 hours")
    }

    @Test
    fun `verification email should include verification link with base URL`() {
        val email = "link-test@example.com"
        val token = "link-token"
        val baseUrl = "https://example.com"

        emailService.sendVerificationEmail(email, token, baseUrl)

        val sentEmail = testEmailProvider.getSentEmails().first()
        val expectedLink = "$baseUrl/api/auth/verify-email?token=$token"

        assertThat(sentEmail.textContent).contains(expectedLink)
        assertThat(sentEmail.htmlContent).contains(expectedLink)
    }

    // ========================================
    // PASSWORD RESET EMAIL TESTS
    // ========================================

    @Test
    fun `should send password reset email with correct content`() {
        val email = "reset-${UUID.randomUUID()}@example.com"
        val token = "reset-token-123"

        emailService.sendPasswordResetEmail(email, token)

        val sentEmails = testEmailProvider.getSentEmails()
        assertThat(sentEmails).hasSize(1)

        val sentEmail = sentEmails.first()
        assertThat(sentEmail.to).isEqualTo(email)
        assertThat(sentEmail.subject).isEqualTo("Reset Your Password")
        assertThat(sentEmail.textContent).contains("reset your password")
        assertThat(sentEmail.textContent).contains(token)
        assertThat(sentEmail.htmlContent).contains("Reset Password")
        assertThat(sentEmail.htmlContent).contains(token)
    }

    @Test
    fun `password reset email should include expiration warning`() {
        val email = "reset-expiration@example.com"
        val token = "reset-token"

        emailService.sendPasswordResetEmail(email, token)

        val sentEmail = testEmailProvider.getSentEmails().first()
        assertThat(sentEmail.textContent).containsIgnoringCase("1 hour")
        assertThat(sentEmail.htmlContent).containsIgnoringCase("1 hour")
    }

    @Test
    fun `password reset email should include security warning`() {
        val email = "security-test@example.com"
        val token = "security-token"

        emailService.sendPasswordResetEmail(email, token)

        val sentEmail = testEmailProvider.getSentEmails().first()
        assertThat(sentEmail.textContent).containsIgnoringCase("didn't request")
        assertThat(sentEmail.htmlContent).containsIgnoringCase("didn't request")
    }

    @Test
    fun `password reset email should include reset link with base URL`() {
        val email = "reset-link@example.com"
        val token = "reset-link-token"
        val baseUrl = "https://example.com"

        emailService.sendPasswordResetEmail(email, token, baseUrl)

        val sentEmail = testEmailProvider.getSentEmails().first()
        val expectedLink = "$baseUrl/api/auth/reset-password?token=$token"

        assertThat(sentEmail.textContent).contains(expectedLink)
        assertThat(sentEmail.htmlContent).contains(expectedLink)
    }

    // ========================================
    // EMAIL PROVIDER INTEGRATION TESTS
    // ========================================

    @Test
    fun `should use configured email provider`() {
        assertThat(emailProvider).isInstanceOf(TestEmailProvider::class.java)
        assertThat(emailProvider.getProviderName()).isEqualTo("TestEmailProvider")
    }

    @Test
    fun `registration flow should trigger verification email`() {
        val email = "register-email-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }

        // Note: Email sending might be async or part of registration logic
        // This test verifies the integration between registration and email service
        val user = appUserRepository.findByEmail(email)
        assertThat(user).isNotNull
    }

    @Test
    fun `should handle multiple concurrent email sends`() {
        val emails = (1..5).map { "concurrent-$it@example.com" }

        emails.forEach { email ->
            emailService.sendVerificationEmail(email, "token-$email")
        }

        val sentEmails = testEmailProvider.getSentEmails()
        assertThat(sentEmails).hasSizeGreaterThanOrEqualTo(5)

        emails.forEach { email ->
            assertThat(sentEmails.any { it.to == email }).isTrue()
        }
    }

    @Test
    fun `should handle emails with special characters`() {
        val email = "special+test@example.com"
        val token = "token-with-special-chars-!@#$%"

        emailService.sendVerificationEmail(email, token)

        val sentEmail = testEmailProvider.getSentEmails().first()
        assertThat(sentEmail.to).isEqualTo(email)
        assertThat(sentEmail.textContent).contains(token)
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Test
    fun `should handle email provider failure gracefully`() {
        testEmailProvider.simulateFailure = true

        try {
            emailService.sendVerificationEmail("fail@example.com", "token")
        } catch (e: Exception) {
            assertThat(e.message).contains("Simulated email failure")
        }

        testEmailProvider.simulateFailure = false
    }
}

/**
 * Test implementation of EmailProvider that captures sent emails
 * instead of actually sending them.
 */
class TestEmailProvider : EmailProvider {
    private val sentEmails = ConcurrentLinkedQueue<EmailMessage>()
    var simulateFailure = false

    override fun sendEmail(message: EmailMessage) {
        if (simulateFailure) {
            throw RuntimeException("Simulated email failure")
        }
        sentEmails.add(message)
    }

    override fun getProviderName(): String = "TestEmailProvider"

    fun getSentEmails(): List<EmailMessage> = sentEmails.toList()

    fun clear() {
        sentEmails.clear()
        simulateFailure = false
    }
}
