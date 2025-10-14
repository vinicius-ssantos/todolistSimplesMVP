package com.viniss.todo.auth


import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*


@Component
class JwtService {
private val secret: String = System.getenv("APP_JWT_SECRET")
?: "dev-secret-please-change-dev-secret-please-change-1234567890"


private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
private val ttlSeconds: Long = 3600 // 1h


fun generateToken(email: String, userId: UUID): String {
val now = Instant.now()
return Jwts.builder()
.subject(email)
.claim("uid", userId.toString())
.issuedAt(Date.from(now))
.expiration(Date.from(now.plusSeconds(ttlSeconds)))
.signWith(key)
.compact()
}


fun extractEmail(token: String): String =
Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject


fun extractUserId(token: String): UUID =
UUID.fromString(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload["uid", String::class.java])


fun isValid(token: String): Boolean = try {
Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
true
} catch (ex: Exception) { false }
}