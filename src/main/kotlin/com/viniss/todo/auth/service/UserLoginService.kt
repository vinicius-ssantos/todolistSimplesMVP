package com.viniss.todo.auth.service

import com.viniss.todo.auth.*
import com.viniss.todo.security.LoginAttemptService
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

/**
 * Service responsible for user authentication (login).
 *
 * Follows Single Responsibility Principle (SRP):
 * - Only handles user login/authentication logic
 * - Separated from registration, token refresh, and logout operations
 *
 * Dependencies reduced from 7 (in AuthService) to 5:
 * - AppUserRepository
 * - PasswordEncoder
 * - TokenService
 * - LoginAttemptService (rate limiting/brute force protection)
 * - RefreshTokenService
 *
 * Benefits:
 * - Clear single responsibility (authentication only)
 * - Easier to test (focused scope)
 * - Easier to extend (e.g., add MFA, OAuth, SSO)
 * - Better maintainability
 * - Security focused (rate limiting, failed attempts tracking)
 */
@Service
class UserLoginService(
    private val userRepository: AppUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
    private val loginAttemptService: LoginAttemptService,
    private val refreshTokenService: RefreshTokenService
) {

    private val logger = LoggerFactory.getLogger(UserLoginService::class.java)

    /**
     * Authenticates a user with email and password.
     *
     * Features:
     * - Rate limiting (blocks after too many failed attempts)
     * - Failed attempt tracking
     * - Email verification warning (doesn't block login)
     *
     * @param email The user's email address
     * @param rawPassword The user's plain text password
     * @return AuthResponseWithRefresh containing access token and refresh token
     * @throws IllegalStateException if account is locked or credentials are invalid
     */
    fun login(email: String, rawPassword: String): AuthResponseWithRefresh {
        logger.debug("Login attempt for email: {}", email)

        // Check if account is locked due to too many failed attempts
        if (loginAttemptService.isBlocked(email)) {
            logger.warn("Login attempt for blocked account: {}", email)
            throw AccountLockedException("Account temporarily locked due to too many failed attempts. Please try again later.")
        }

        // Find user
        val user = userRepository.findByEmail(email)

        // Validate credentials
        if (user == null || !passwordEncoder.matches(rawPassword, user.passwordHash)) {
            loginAttemptService.recordFailedAttempt(email)
            val remainingAttempts = loginAttemptService.getRemainingAttempts(email)
            logger.warn("Failed login attempt for email: {}. Remaining attempts: {}", email, remainingAttempts)
            error("invalid credentials")
        }

        // Check email verification (warning only, doesn't block login)
        if (!user.emailVerified) {
            logger.warn("User logged in with unverified email: {}", email)
        }

        // Successful login - reset failed attempts
        loginAttemptService.resetFailedAttempts(email)
        logger.info("User logged in successfully: {} (id: {})", email, user.id)

        // Generate tokens
        val accessToken = tokenService.generateToken(email = user.email, userId = user.id)
        val refreshToken = refreshTokenService.createRefreshToken(user.id)

        logger.debug("Generated tokens for user: {}", email)

        return AuthResponseWithRefresh(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
}
