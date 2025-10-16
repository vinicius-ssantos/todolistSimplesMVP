package com.viniss.todo.auth


import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey


@Component
class JwtService(private val props: JwtProps) {

    private val secret: String =
        System.getenv("APP_JWT_SECRET") ?: "dev-secret-please-change-dev-secret-please-change-1234567890"


    private val key: SecretKey = run {
        val bytes = Base64.getDecoder().decode(props.secretB64)
        require(bytes.size >= 48) { "JWT secret must be >= 48 bytes after Base64 decode" }
        Keys.hmacShaKeyFor(bytes) // jjwt 0.12.x
    }

    fun generateToken(userId: UUID, email: String): String {
        val now = Instant.now()
        val exp = now.plusSeconds(props.ttlSeconds)
        return Jwts.builder()
            .id(UUID.randomUUID().toString())               // jti
            .subject(userId.toString())                     // sub = UUID
            .issuer(props.issuer)                           // iss
            .audience().add(props.audience).and()                // aud
            .issuedAt(Date.from(now))              // iat
            .notBefore(Date.from(now))             // nbf
            .expiration(Date.from(exp))            // exp
            .claim("email", email)
            .claim("v", props.version)
            // .claim("scp", listOf("user"))                      // opcional
            .signWith(key, Jwts.SIG.HS384)             // fixa HS384
            .compact()
    }

    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .requireIssuer(props.issuer)
            .requireAudience(props.audience)
            .clockSkewSeconds(props.clockSkewSeconds)
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload


    fun isValid(token: String): Boolean = runCatching { parseClaims(token) }.isSuccess

    fun extractUserId(token: String): UUID = UUID.fromString(parseClaims(token).subject)

    fun extractEmail(token: String): String = parseClaims(token)["email", String::class.java]





}