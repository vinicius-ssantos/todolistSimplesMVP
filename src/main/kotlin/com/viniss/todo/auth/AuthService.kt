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
    private val loginAttemptService: LoginAttemptService
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    fun register(email: String, rawPassword: String): AuthResponse {
        require(email.isNotBlank()) { "email is required" }
        require(rawPassword.length >= 6) { "password must have at least 6 chars" }
        if (repo.existsByEmail(email)) {
            logger.warn("Registration attempt with existing email: {}", email)
            error("email already registered")
        }

        val user = repo.save(AppUserEntity(email = email, passwordHash = encoder.encode(rawPassword)))
        logger.info("User registered successfully: {}", email)

        val token = jwt.generateToken(email = user.email, userId = user.id)
        return AuthResponse(token)
    }


    fun login(email: String, rawPassword: String): AuthResponse {
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

        // Successful login - reset failed attempts
        loginAttemptService.resetFailedAttempts(email)
        logger.info("User logged in successfully: {}", email)

        val token = jwt.generateToken(email = user.email, userId = user.id)
        return AuthResponse(token)
    }
}
