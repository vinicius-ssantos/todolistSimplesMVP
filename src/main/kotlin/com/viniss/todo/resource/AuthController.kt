package com.viniss.todo.resource


import com.viniss.todo.auth.*
import com.viniss.todo.auth.service.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID


/**
 * REST Controller for authentication endpoints.
 *
 * Refactored to follow Single Responsibility Principle (SRP):
 * - Uses 4 focused services instead of 1 monolithic AuthService
 * - Each endpoint delegates to a single-purpose service
 *
 * Services used:
 * - UserRegistrationService: /register
 * - UserLoginService: /login
 * - AccessTokenRefreshService: /refresh
 * - UserLogoutService: /logout
 * - EmailVerificationService: /verify-email, /resend-verification
 *
 * Benefits:
 * - Clearer dependencies (each endpoint uses focused service)
 * - Better separation of concerns
 * - Easier to test endpoints independently
 * - Easier to extend (e.g., add OAuth, MFA, SSO)
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRegistrationService: UserRegistrationService,
    private val userLoginService: UserLoginService,
    private val accessTokenRefreshService: AccessTokenRefreshService,
    private val userLogoutService: UserLogoutService,
    private val emailVerificationService: EmailVerificationService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody req: AuthRequest): ResponseEntity<AuthResponseWithRefresh> =
        ResponseEntity.ok(userRegistrationService.register(req.email.trim(), req.password))


    @PostMapping("/login")
    fun login(@Valid @RequestBody req: AuthRequest): ResponseEntity<AuthResponseWithRefresh> =
        ResponseEntity.ok(userLoginService.login(req.email.trim(), req.password))


    @PostMapping("/refresh")
    fun refresh(@RequestBody req: RefreshTokenRequest): ResponseEntity<AuthResponseWithRefresh> =
        ResponseEntity.ok(accessTokenRefreshService.refreshAccessToken(req.refreshToken))


    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authHeader: String,
        @AuthenticationPrincipal userId: UUID?
    ): ResponseEntity<Map<String, String>> {
        requireNotNull(userId) { "User ID not found in authentication context" }
        val token = authHeader.removePrefix("Bearer ").trim()
        userLogoutService.logout(userId, token)
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }


    @GetMapping("/verify-email")
    fun verifyEmail(@RequestParam token: String): ResponseEntity<VerifyEmailResponse> {
        val success = emailVerificationService.verifyEmail(token)
        val message = if (success) {
            "Email verified successfully!"
        } else {
            "Invalid or expired verification token"
        }
        return ResponseEntity.ok(VerifyEmailResponse(success, message))
    }


    @PostMapping("/resend-verification")
    fun resendVerification(@AuthenticationPrincipal userId: UUID): ResponseEntity<Map<String, String>> {
        emailVerificationService.generateAndSendVerificationToken(userId)
        return ResponseEntity.ok(mapOf("message" to "Verification email sent"))
    }
}
