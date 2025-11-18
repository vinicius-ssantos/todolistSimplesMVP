package com.viniss.todo.resource


import com.viniss.todo.auth.AuthRequest
import com.viniss.todo.auth.AuthResponse
import com.viniss.todo.auth.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/auth")
class AuthController(
private val service: AuthService
) {
@PostMapping("/register")
fun register(@Valid @RequestBody req: AuthRequest): ResponseEntity<AuthResponse> =
ResponseEntity.ok(service.register(req.email.trim(), req.password))


@PostMapping("/login")
fun login(@Valid @RequestBody req: AuthRequest): ResponseEntity<AuthResponse> =
ResponseEntity.ok(service.login(req.email.trim(), req.password))
}