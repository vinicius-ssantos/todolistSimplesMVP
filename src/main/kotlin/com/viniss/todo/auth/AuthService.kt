package com.viniss.todo.auth


import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


@Service
class AuthService(
    private val repo: AppUserRepository,
    private val encoder: PasswordEncoder,
    private val jwt: JwtService
) {
    fun register(email: String, rawPassword: String): AuthResponse {
        require(email.isNotBlank()) { "email is required" }
        require(rawPassword.length >= 6) { "password must have at least 6 chars" }
        if (repo.existsByEmail(email)) error("email already registered")


        val user = repo.save(AppUserEntity(email = email, passwordHash = encoder.encode(rawPassword)))
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