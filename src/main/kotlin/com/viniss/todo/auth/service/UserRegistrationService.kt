package com.viniss.todo.auth.service

import com.viniss.todo.auth.*
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

/**
 * Service responsible for user registration.
 *
 * Follows Single Responsibility Principle (SRP):
 * - Only handles user registration logic
 * - Separated from login, token refresh, and logout operations
 *
 * Dependencies reduced from 7 (in AuthService) to 6:
 * - AppUserRepository
 * - PasswordEncoder
 * - TokenService
 * - PasswordValidator
 * - PasswordHistoryService
 * - RefreshTokenService
 * - EmailVerificationService
 *
 * Benefits:
 * - Clear single responsibility (user registration only)
 * - Easier to test (focused scope)
 * - Easier to extend (e.g., add email verification, OAuth registration)
 * - Better maintainability
 */
@Service
class UserRegistrationService(
    private val userRepository: AppUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
    private val passwordValidator: PasswordValidator,
    private val passwordHistoryService: PasswordHistoryService,
    private val refreshTokenService: RefreshTokenService,
    private val emailVerificationService: EmailVerificationService
) {

    private val logger = LoggerFactory.getLogger(UserRegistrationService::class.java)

    /**
     * Registers a new user with email and password.
     *
     * @param email The user's email address
     * @param rawPassword The user's plain text password
     * @return AuthResponseWithRefresh containing access token and refresh token
     * @throws IllegalArgumentException if email is blank
     * @throws IllegalStateException if email already exists or password validation fails
     */
    fun register(email: String, rawPassword: String): AuthResponseWithRefresh {
        require(email.isNotBlank()) { "email is required" }

        logger.info("Registration attempt for email: {}", email)

        // Validate password strength and complexity
        val validationResult = passwordValidator.validate(rawPassword)
        if (!validationResult.isValid()) {
            val errors = validationResult.errorList().joinToString("; ")
            logger.warn("Password validation failed for email {}: {}", email, errors)
            error("Password validation failed: $errors")
        }

        // Check if email already exists
        if (userRepository.existsByEmail(email)) {
            logger.warn("Registration failed: email already registered: {}", email)
            error("email already registered")
        }

        // Create user
        val passwordHash = passwordEncoder.encode(rawPassword)
        val user = userRepository.save(AppUserEntity(email = email, passwordHash = passwordHash))

        logger.info("User registered successfully: {} (id: {})", email, user.id)

        // Record password in history for future validation
        passwordHistoryService.recordPasswordChange(user.id, passwordHash)

        // Generate email verification token
        emailVerificationService.generateAndSendVerificationToken(user.id)

        // Generate tokens
        val accessToken = tokenService.generateToken(email = user.email, userId = user.id)
        val refreshToken = refreshTokenService.createRefreshToken(user.id)

        logger.debug("Generated tokens for new user: {}", email)

        return AuthResponseWithRefresh(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }
}
