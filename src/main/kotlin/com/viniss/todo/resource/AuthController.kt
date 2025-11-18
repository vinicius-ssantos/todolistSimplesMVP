package com.viniss.todo.resource


import com.viniss.todo.auth.*
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID


@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val service: AuthService,
    private val emailVerificationService: EmailVerificationService,
    private val tokenBlacklistService: TokenBlacklistService
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody req: AuthRequest): ResponseEntity<AuthResponseWithRefresh> =
        ResponseEntity.ok(service.register(req.email.trim(), req.password))


    @PostMapping("/login")
    fun login(@Valid @RequestBody req: AuthRequest): ResponseEntity<AuthResponseWithRefresh> =
        ResponseEntity.ok(service.login(req.email.trim(), req.password))


    @PostMapping("/refresh")
    fun refresh(@RequestBody req: RefreshTokenRequest): ResponseEntity<AuthResponseWithRefresh> =
        ResponseEntity.ok(service.refreshAccessToken(req.refreshToken))


    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authHeader: String,
        @AuthenticationPrincipal userId: UUID
    ): ResponseEntity<Map<String, String>> {
        val token = authHeader.removePrefix("Bearer ").trim()
        tokenBlacklistService.blacklistToken(token, userId, "logout")
        service.logout(userId, token)
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