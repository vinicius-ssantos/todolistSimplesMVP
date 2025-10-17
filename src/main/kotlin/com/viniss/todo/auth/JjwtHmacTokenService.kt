package com.viniss.todo.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Clock
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

/**
 * HS384 implementation backed by JJWT.
 * Enforces issuer, audience, and standard temporal checks with configurable skew.
 */
class JjwtHmacTokenService(
    private val props: JwtProps,
    private val nowProvider: () -> Instant = { Instant.now() }
) : TokenService {

    private val key: SecretKey = Keys.hmacShaKeyFor(props.hmacSecretBytes)

    override fun generateToken(userId: UUID, email: String): String {
        val now = nowProvider()
        val exp = now.plusSeconds(props.ttlSeconds)
        return Jwts.builder()
            .id(UUID.randomUUID().toString())               // jti
            .subject(userId.toString())                     // sub = UUID
            .issuer(props.issuer)                           // iss
            .audience().add(props.audience).and()           // aud
            .issuedAt(Date.from(now))                       // iat
            .notBefore(Date.from(now))                      // nbf
            .expiration(Date.from(exp))                     // exp
            .claim("email", email)
            .claim("v", props.version)
            .signWith(key, Jwts.SIG.HS384)                  // HS384 only
            .compact()
    }

    private fun parseClaims(token: String): Claims {
        val jws = Jwts.parser()
            .clockSkewSeconds(props.clockSkewSeconds)
            .clock(Clock { Date.from(nowProvider()) })
            .verifyWith(key)
            .requireIssuer(props.issuer)
            .requireAudience(props.audience)
            .build()
            .parseSignedClaims(token)

        if (jws.header.algorithm != Jwts.SIG.HS384.id) {
            throw InvalidTokenException("Unsupported alg: ${jws.header.algorithm}")
        }

        val claims = jws.payload
        val now = nowProvider()
        val issuedAt = claims.issuedAt?.toInstant() ?: throw InvalidTokenException("Missing iat")
        if (issuedAt.isAfter(now.plusSeconds(props.clockSkewSeconds))) {
            throw InvalidTokenException("iat in the future")
        }
        return claims
    }

    override fun isValid(token: String): Boolean = runCatching { parseClaims(token) }.isSuccess

    override fun extractUserId(token: String): UUID =
        runCatching { UUID.fromString(parseClaims(token).subject) }
            .getOrElse { throw InvalidTokenException("sub is not a valid UUID") }

    override fun extractEmail(token: String): String =
        parseClaims(token)["email", String::class.java]
}
