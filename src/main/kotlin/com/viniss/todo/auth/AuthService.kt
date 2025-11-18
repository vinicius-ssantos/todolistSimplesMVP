package com.viniss.todo.auth


import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


@Service
class AuthService(
    private val repo: AppUserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: TokenService,
    private val passwordValidator: PasswordValidator,
    private val passwordHistoryService: PasswordHistoryService
) {
    fun register(email: String, rawPassword: String): AuthResponse {
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

        val token = jwt.generateToken(email = user.email, userId = user.id)
        return AuthResponse(token)
    }


    fun login(email: String, rawPassword: String): AuthResponse {
        val user = repo.findByEmail(email) ?: error("invalid credentials")
        if (!encoder.matches(rawPassword, user.passwordHash)) error("invalid credentials")
        val token = jwt.generateToken(email = user.email, userId = user.id)
        return AuthResponse(token)
    }
}
