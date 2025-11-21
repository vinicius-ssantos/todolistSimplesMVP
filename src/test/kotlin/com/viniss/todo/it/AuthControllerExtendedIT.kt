package com.viniss.todo.it

import com.fasterxml.jackson.databind.ObjectMapper
import com.viniss.todo.auth.*
import com.viniss.todo.auth.maintenance.RefreshTokenMaintenanceService
import com.viniss.todo.auth.maintenance.TokenBlacklistMaintenanceService
import com.viniss.todo.repo.TaskRepository
import com.viniss.todo.repo.TodoListRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

/**
 * Extended integration tests for AuthController.
 *
 * These tests provide comprehensive coverage for:
 * - UserRegistrationService
 * - UserLoginService
 * - AccessTokenRefreshService
 * - UserLogoutService
 * - RefreshTokenService
 * - TokenBlacklistService
 * - PasswordValidator (via registration)
 * - PasswordHistoryService (via registration)
 *
 * Uses @ParameterizedTest to efficiently cover multiple scenarios.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AuthControllerExtendedIT {

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var objectMapper: ObjectMapper
    @Autowired protected lateinit var todoListRepository: TodoListRepository
    @Autowired protected lateinit var taskRepository: TaskRepository
    @Autowired protected lateinit var appUserRepository: AppUserRepository
    @Autowired protected lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired protected lateinit var blacklistedTokenRepository: BlacklistedTokenRepository
    @Autowired protected lateinit var rateLimitService: com.viniss.todo.security.RateLimitService
    @Autowired protected lateinit var tokenBlacklistService: TokenBlacklistService
    @Autowired protected lateinit var refreshTokenMaintenanceService: RefreshTokenMaintenanceService
    @Autowired protected lateinit var tokenBlacklistMaintenanceService: TokenBlacklistMaintenanceService

    @BeforeEach
    fun cleanDatabase() {
        rateLimitService.reset()
        taskRepository.deleteAll()
        todoListRepository.deleteAll()
        blacklistedTokenRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        appUserRepository.deleteAll()
    }

    // ========================================
    // REGISTRATION TESTS
    // ========================================

    @Test
    fun `POST register should successfully register user with valid password`() {
        val email = "newuser-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        val response = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val authResponse = objectMapper.readValue(
            response.response.contentAsString,
            AuthResponseWithRefresh::class.java
        )

        assertThat(authResponse.accessToken).isNotBlank()
        assertThat(authResponse.refreshToken).isNotBlank()
        assertThat(authResponse.expiresIn).isEqualTo(900)

        // Verify user was created in database
        val user = appUserRepository.findByEmail(email)
        assertThat(user).isNotNull
        assertThat(user!!.email).isEqualTo(email)
    }

    @ParameterizedTest
    @CsvSource(
        "short, 'Password must be at least 12 characters'",
        "nouppercasepassword123!, 'must contain at least one uppercase letter'",
        "NOLOWERCASE123!, 'must contain at least one lowercase letter'",
        "NoDigitsPassword!, 'must contain at least one digit'",
        "NoSpecialChar123, 'must contain at least one special character'",
        "password12345ABC!, 'contains common weak patterns'",
        "Abc123456789!, 'contains sequential characters'"
    )
    fun `POST register should reject invalid passwords`(password: String, expectedErrorSubstring: String) {
        val email = "test-${UUID.randomUUID()}@example.com"

        val response = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isBadRequest() }
        }.andReturn()

        val errorMessage = response.response.contentAsString
        assertThat(errorMessage).containsIgnoringCase(expectedErrorSubstring)
    }

    @Test
    fun `POST register should reject duplicate email`() {
        val email = "duplicate-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        // First registration
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }

        // Second registration with same email
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isBadRequest() }
        }.andReturn().let { response ->
            assertThat(response.response.contentAsString)
                .containsIgnoringCase("email already registered")
        }
    }

    @Test
    fun `POST register should create refresh token in database`() {
        val email = "refreshtest-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        val response = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val authResponse = objectMapper.readValue(
            response.response.contentAsString,
            AuthResponseWithRefresh::class.java
        )

        // Verify refresh token was saved
        val user = appUserRepository.findByEmail(email)!!
        val refreshTokens = refreshTokenRepository.findByUserId(user.id)
        assertThat(refreshTokens).hasSize(1)
        assertThat(refreshTokens.first().token).isEqualTo(authResponse.refreshToken)
    }

    // ========================================
    // LOGIN TESTS
    // ========================================

    @Test
    fun `POST login should successfully authenticate with valid credentials`() {
        val email = "logintest-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        // Register first
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }

        // Login
        val response = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val authResponse = objectMapper.readValue(
            response.response.contentAsString,
            AuthResponseWithRefresh::class.java
        )

        assertThat(authResponse.accessToken).isNotBlank()
        assertThat(authResponse.refreshToken).isNotBlank()
    }

    @ParameterizedTest
    @CsvSource(
        "wrong@example.com, WrongP@ss123",
        "nonexistent@example.com, SecureP@ss2024"
    )
    fun `POST login should reject invalid credentials`(email: String, password: String) {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isBadRequest() }
        }.andReturn().let { response ->
            assertThat(response.response.contentAsString)
                .containsIgnoringCase("invalid credentials")
        }
    }

    @Test
    fun `POST login should reject wrong password for existing user`() {
        val email = "wrongpasstest-${UUID.randomUUID()}@example.com"
        val correctPassword = "SecureP@ss2024"
        val wrongPassword = "WrongP@ss2024"

        // Register
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$correctPassword"}"""
        }

        // Login with wrong password
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$wrongPassword"}"""
        }.andExpect {
            status { isBadRequest() }
        }.andReturn().let { response ->
            assertThat(response.response.contentAsString)
                .containsIgnoringCase("invalid credentials")
        }
    }

    @Test
    fun `POST login should enforce rate limiting after multiple failed attempts`() {
        val email = "ratelimit-${UUID.randomUUID()}@example.com"
        val password = "WrongP@ss123"

        // Make 5 failed login attempts
        repeat(5) {
            mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"$email","password":"$password"}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }

        // 6th attempt should be rate limited
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isTooManyRequests() }
        }.andReturn().let { response ->
            assertThat(response.response.contentAsString)
                .containsIgnoringCase("locked")
        }
    }

    @Test
    fun `POST login should reset failed attempts on successful login`() {
        val email = "resetattempt-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        // Register
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }

        // Make some failed attempts
        repeat(3) {
            mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"$email","password":"WrongP@ss123"}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }

        // Successful login
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }

        // Failed attempts should be reset - should be able to make more attempts
        repeat(3) {
            mockMvc.post("/api/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"$email","password":"WrongP@ss123"}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    // ========================================
    // REFRESH TOKEN TESTS
    // ========================================

    @Test
    fun `POST refresh should generate new access token with valid refresh token`() {
        val email = "refreshtest-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        // Register to get refresh token
        val registerResponse = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andReturn()

        val registerAuth = objectMapper.readValue(
            registerResponse.response.contentAsString,
            AuthResponseWithRefresh::class.java
        )

        // Use refresh token to get new access token
        val refreshResponse = mockMvc.post("/api/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"${registerAuth.refreshToken}"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val refreshAuth = objectMapper.readValue(
            refreshResponse.response.contentAsString,
            AuthResponseWithRefresh::class.java
        )

        assertThat(refreshAuth.accessToken).isNotBlank()
        assertThat(refreshAuth.refreshToken).isNotBlank()
        // New access token should be different
        assertThat(refreshAuth.accessToken).isNotEqualTo(registerAuth.accessToken)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "invalid-token",
        "eyJhbGciOiJIUzI1NiJ9.invalid.signature",
        ""
    ])
    fun `POST refresh should reject invalid refresh tokens`(invalidToken: String) {
        mockMvc.post("/api/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"$invalidToken"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST refresh should reject expired refresh token`() {
        val email = "expiredrefresh-${UUID.randomUUID()}@example.com"

        // Create user
        val user = appUserRepository.save(
            AppUserEntity(email = email, passwordHash = "hash")
        )

        // Create expired refresh token
        val expiredToken = refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = user.id,
                token = "expired-token-${UUID.randomUUID()}",
                expiresAt = Instant.now().minusSeconds(3600) // Expired 1 hour ago
            )
        )

        mockMvc.post("/api/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"${expiredToken.token}"}"""
        }.andExpect {
            status { isBadRequest() }
        }.andReturn().let { response ->
            assertThat(response.response.contentAsString)
                .containsIgnoringCase("expired")
        }
    }

    // ========================================
    // LOGOUT TESTS
    // ========================================

    @Test
    fun `POST logout should blacklist access token`() {
        val email = "logouttest-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        // Register
        val registerResponse = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andReturn()

        val authResponse = objectMapper.readValue(
            registerResponse.response.contentAsString,
            AuthResponseWithRefresh::class.java
        )

        // Verify token works before logout
        mockMvc.get("/v1/lists") {
            header("Authorization", "Bearer ${authResponse.accessToken}")
        }.andExpect {
            status { isOk() }
        }

        // Logout
        mockMvc.post("/api/auth/logout") {
            header("Authorization", "Bearer ${authResponse.accessToken}")
        }.andExpect {
            status { isOk() }
        }

        // Verify token is blacklisted
        val user = appUserRepository.findByEmail(email)!!
        val blacklistedTokens = blacklistedTokenRepository.findByUserId(user.id)
        assertThat(blacklistedTokens).isNotEmpty()
    }

    @Test
    fun `POST logout should invalidate user's refresh tokens`() {
        val email = "logoutrefresh-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        // Register
        val registerResponse = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andReturn()

        val authResponse = objectMapper.readValue(
            registerResponse.response.contentAsString,
            AuthResponseWithRefresh::class.java
        )

        // Logout
        mockMvc.post("/api/auth/logout") {
            header("Authorization", "Bearer ${authResponse.accessToken}")
        }.andExpect {
            status { isOk() }
        }

        // Try to use refresh token after logout
        mockMvc.post("/api/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"${authResponse.refreshToken}"}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `cannot access protected endpoints after logout`() {
        val email = "protectedlogout-${UUID.randomUUID()}@example.com"
        val password = "SecureP@ss2024"

        // Register
        val registerResponse = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andReturn()

        val authResponse = objectMapper.readValue(
            registerResponse.response.contentAsString,
            AuthResponseWithRefresh::class.java
        )

        // Logout
        mockMvc.post("/api/auth/logout") {
            header("Authorization", "Bearer ${authResponse.accessToken}")
        }.andExpect {
            status { isOk() }
        }

        // Try to access protected endpoint
        mockMvc.get("/v1/lists") {
            header("Authorization", "Bearer ${authResponse.accessToken}")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    // ========================================
    // TOKEN MAINTENANCE TESTS
    // ========================================

    @Test
    fun `RefreshTokenMaintenanceService should cleanup expired tokens`() {
        val email = "cleanuptest-${UUID.randomUUID()}@example.com"

        // Create user
        val user = appUserRepository.save(
            AppUserEntity(email = email, passwordHash = "hash")
        )

        // Create some expired tokens
        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = user.id,
                token = "expired1-${UUID.randomUUID()}",
                expiresAt = Instant.now().minusSeconds(3600)
            )
        )
        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = user.id,
                token = "expired2-${UUID.randomUUID()}",
                expiresAt = Instant.now().minusSeconds(7200)
            )
        )

        // Create active token
        refreshTokenRepository.save(
            RefreshTokenEntity(
                userId = user.id,
                token = "active-${UUID.randomUUID()}",
                expiresAt = Instant.now().plusSeconds(3600)
            )
        )

        // Run cleanup
        val deletedCount = refreshTokenMaintenanceService.cleanupExpiredTokensNow()

        assertThat(deletedCount).isEqualTo(2)

        // Verify only active token remains
        val remainingTokens = refreshTokenRepository.findByUserId(user.id)
        assertThat(remainingTokens).hasSize(1)
        assertThat(remainingTokens.first().token).startsWith("active-")
    }

    @Test
    fun `TokenBlacklistMaintenanceService should cleanup expired blacklisted tokens`() {
        val email = "blacklistcleanup-${UUID.randomUUID()}@example.com"

        // Create user
        val user = appUserRepository.save(
            AppUserEntity(email = email, passwordHash = "hash")
        )

        // Create expired blacklisted tokens
        blacklistedTokenRepository.save(
            BlacklistedTokenEntity(
                userId = user.id,
                tokenJti = "expired-jti-1",
                expiresAt = Instant.now().minusSeconds(3600)
            )
        )
        blacklistedTokenRepository.save(
            BlacklistedTokenEntity(
                userId = user.id,
                tokenJti = "expired-jti-2",
                expiresAt = Instant.now().minusSeconds(7200)
            )
        )

        // Create active blacklisted token
        blacklistedTokenRepository.save(
            BlacklistedTokenEntity(
                userId = user.id,
                tokenJti = "active-jti",
                expiresAt = Instant.now().plusSeconds(3600)
            )
        )

        // Run cleanup
        val deletedCount = tokenBlacklistMaintenanceService.cleanupExpiredTokensNow()

        assertThat(deletedCount).isEqualTo(2)

        // Verify only active token remains
        val remainingTokens = blacklistedTokenRepository.findByUserId(user.id)
        assertThat(remainingTokens).hasSize(1)
        assertThat(remainingTokens.first().tokenJti).isEqualTo("active-jti")
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 5, 10, 50])
    fun `RefreshTokenMaintenanceService should handle varying token counts`(tokenCount: Int) {
        val email = "varyingcount-${UUID.randomUUID()}@example.com"
        val user = appUserRepository.save(
            AppUserEntity(email = email, passwordHash = "hash")
        )

        // Create expired tokens
        repeat(tokenCount) { i ->
            refreshTokenRepository.save(
                RefreshTokenEntity(
                    userId = user.id,
                    token = "expired-$i-${UUID.randomUUID()}",
                    expiresAt = Instant.now().minusSeconds(3600)
                )
            )
        }

        val deletedCount = refreshTokenMaintenanceService.cleanupExpiredTokensNow()
        assertThat(deletedCount).isEqualTo(tokenCount.toLong())
    }

    @Test
    fun `RefreshTokenMaintenanceService should provide token statistics`() {
        val email = "stats-${UUID.randomUUID()}@example.com"
        val user = appUserRepository.save(
            AppUserEntity(email = email, passwordHash = "hash")
        )

        // Create mix of active and expired tokens
        repeat(3) { i ->
            refreshTokenRepository.save(
                RefreshTokenEntity(
                    userId = user.id,
                    token = "active-$i-${UUID.randomUUID()}",
                    expiresAt = Instant.now().plusSeconds(3600)
                )
            )
        }
        repeat(2) { i ->
            refreshTokenRepository.save(
                RefreshTokenEntity(
                    userId = user.id,
                    token = "expired-$i-${UUID.randomUUID()}",
                    expiresAt = Instant.now().minusSeconds(3600)
                )
            )
        }

        val stats = refreshTokenMaintenanceService.getTokenStatistics()

        assertThat(stats["total"]).isGreaterThanOrEqualTo(5)
        assertThat(stats["active"]).isGreaterThanOrEqualTo(3)
        assertThat(stats["expired"]).isGreaterThanOrEqualTo(2)
    }
}
