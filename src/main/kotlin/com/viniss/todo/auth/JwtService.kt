package com.viniss.todo.auth


import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

/**
 * TokenService baseado em JJWT para HS384.
 * - Chave é derivada apenas de JwtProps.hmacSecretBytes (Base64 URL-safe/normal já tratado em JwtProps)
 * - Valida iss, aud, exp/nbf/iat com clock skew configurável
 * - sub deve ser um UUID; falha de parse -> IllegalArgumentException (capturada por JwtAuthFilter)
 */
@Component
class JwtService(private val props: JwtProps) {

    private val key: SecretKey = Keys.hmacShaKeyFor(props.hmacSecretBytes)

    fun generateToken(userId: UUID, email: String): String {
        val now = Instant.now()
        val exp = now.plusSeconds(props.ttlSeconds)
        return Jwts.builder()
            .id(UUID.randomUUID().toString())               // jti
            .subject(userId.toString())                     // sub = UUID
            .issuer(props.issuer)                           // iss
            .audience().add(props.audience).and()          // aud
            .issuedAt(Date.from(now))                      // iat
            .notBefore(Date.from(now))                     // nbf
            .expiration(Date.from(exp))                    // exp
            .claim("email", email)
            .claim("v", props.version)
            .signWith(key, Jwts.SIG.HS384)                 // fixa HS384
            .compact()
    }

    private fun parserBuilder() = Jwts.parser()
        .clockSkewSeconds(props.clockSkewSeconds)
        .verifyWith(key)
        .requireIssuer(props.issuer)
        .requireAudience(props.audience)

    private fun parseClaims(token: String): Claims =
        parserBuilder()
            .build()
            .parseSignedClaims(token)
            .payload

    fun isValid(token: String): Boolean = runCatching { parseClaims(token) }.isSuccess

    fun extractUserId(token: String): UUID = UUID.fromString(parseClaims(token).subject)

    fun extractEmail(token: String): String = parseClaims(token)["email", String::class.java]





}