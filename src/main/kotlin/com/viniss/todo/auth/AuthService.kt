package com.viniss.todo.auth

import com.viniss.todo.security.LoginAttemptService
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


@Service
class AuthService(
    private val repo: AppUserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: TokenService,
    private val passwordValidator: PasswordValidator,
    private val passwordHistoryService: PasswordHistoryService,
    private val loginAttemptService: LoginAttemptService,
    private val refreshTokenService: RefreshTokenService
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun register(email: String, rawPassword: String): AuthResponseWithRefresh {
        require(email.isNotBlank()) { "email is required" }

        // Validate password strength and complexity
        val validationResult = passwordValidator.validate(rawPassword)
        if (!validationResult.isValid()) {
            val errors = validationResult.errorList().joinToString("; ")
            error("Password validation failed: $errors")
        }

        if (repo.existsByEmail(email)) error("email already registered")

        val passwordHash = encoder.encode(rawPassword)
        val user = repo.save(AppUserEntity(email = email, passwordHash = passwordHash))

        // Record password in history
        passwordHistoryService.recordPasswordChange(user.id, passwordHash)

        val accessToken = jwt.generateToken(email = user.email, userId = user.id)
        val refreshToken = refreshTokenService.createRefreshToken(user.id)

        return AuthResponseWithRefresh(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }


    fun login(email: String, rawPassword: String): AuthResponseWithRefresh {
        // Check if account is locked
        if (loginAttemptService.isBlocked(email)) {
            logger.warn("Login attempt for blocked account: {}", email)
            error("Account temporarily locked due to too many failed attempts. Please try again later.")
        }

        val user = repo.findByEmail(email)

        if (user == null || !encoder.matches(rawPassword, user.passwordHash)) {
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
        logger.info("User logged in successfully: {}", email)

        val accessToken = jwt.generateToken(email = user.email, userId = user.id)
        val refreshToken = refreshTokenService.createRefreshToken(user.id)

        return AuthResponseWithRefresh(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    fun refreshAccessToken(refreshToken: String): AuthResponseWithRefresh {
        val userId = refreshTokenService.validateAndGetUserId(refreshToken)
            ?: run {
                logger.warn("Invalid or expired refresh token")
                error("Invalid refresh token")
            }

        val user = repo.findById(userId)
            .orElseThrow { error("User not found") }

        val newAccessToken = jwt.generateToken(email = user.email, userId = user.id)
        val newRefreshToken = refreshTokenService.createRefreshToken(user.id)

        // Revoke old refresh token
        refreshTokenService.revokeToken(refreshToken)

        logger.info("Access token refreshed for user: ${user.email}")

        return AuthResponseWithRefresh(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }

    fun logout(userId: java.util.UUID, accessToken: String) {
        // Blacklist current access token
        // Note: TokenBlacklistService will be injected when integrated with JwtAuthFilter

        // Revoke all refresh tokens for user
        refreshTokenService.revokeAllUserTokens(userId)

        logger.info("User logged out: $userId")
    }
}
