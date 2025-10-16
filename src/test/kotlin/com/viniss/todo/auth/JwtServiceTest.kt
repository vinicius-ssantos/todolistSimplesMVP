package com.viniss.todo.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.time.Instant
import java.util.*
import java.util.Base64.getEncoder
import javax.crypto.SecretKey


class JwtServiceTest {

    private fun props(
        issuer: String = "tickr-api",
        audience: String = "tickr-web",
        secretB64: String = strongSecretB64(),
        ttlSeconds: Long = 3600,
        skewSeconds: Long = 60,
        version: Int = 1
    ) = JwtProps(
        issuer = issuer,
        audience = audience,
        secretB64 = secretB64,
        ttlSeconds = ttlSeconds,
        clockSkewSeconds = skewSeconds,
        version = version
    )

    @Test
    fun `token contains required claims and validates`() {
        val jwt = JwtService(props())
        val userId = UUID.randomUUID()

        val token = jwt.generateToken(userId, "dev@example.com")

        assertTrue(jwt.isValid(token), "Token should be valid")
        assertEquals(userId, jwt.extractUserId(token))
        assertEquals("dev@example.com", jwt.extractEmail(token))

        // Confere header.alg = HS384 (decodificando a 1ª parte do JWT)
        val headerJson = String(Base64.getUrlDecoder().decode(token.substringBefore(".")))
        assertTrue(headerJson.contains("\"HS384\""), "alg should be HS384")
    }

    @Test
    fun `issuer mismatch should invalidate`() {
        val secret = strongSecretB64()
        val good = JwtService(props(issuer = "tickr-api", secretB64 = secret))
        val bad = JwtService(props(issuer = "other-issuer", secretB64 = secret))

        val t = good.generateToken(UUID.randomUUID(), "a@b.com")
        assertFalse(bad.isValid(t), "Different issuer must be rejected")
    }

    @Test
    fun `audience mismatch should invalidate`() {
        val secret = strongSecretB64()
        val good = JwtService(props(audience = "tickr-web", secretB64 = secret))
        val bad = JwtService(props(audience = "other-aud", secretB64 = secret))

        val t = good.generateToken(UUID.randomUUID(), "a@b.com")
        assertFalse(bad.isValid(t), "Different audience must be rejected")
    }

    @Test
    fun `skew within +60s future iat-nbf should be accepted`() {
        val p = props(skewSeconds = 60)
        val jwt = JwtService(p)
        val futureAt = Instant.now().plusSeconds(30) // dentro do skew

        val token = buildTokenAt(futureAt, p, UUID.randomUUID(), "a@b.com")
        assertTrue(jwt.isValid(token), "Future token within skew should be valid")
    }

    @Test
    fun `skew beyond +60s future iat-nbf should be rejected`() {
        val p = props(skewSeconds = 60)
        val jwt = JwtService(p)
        val futureAt = Instant.now().plusSeconds(120) // fora do skew

        val token = buildTokenAt(futureAt, p, UUID.randomUUID(), "a@b.com")
        assertFalse(jwt.isValid(token), "Future token beyond skew must be invalid")
    }

    @Test
    fun `weak secret (lt 48 bytes) should fail at service init`() {
        // 16 bytes → muito fraco
        val weak = getEncoder().encodeToString(ByteArray(16))
        val ex = assertThrows(IllegalArgumentException::class.java) {
            JwtService(props(secretB64 = weak))
        }
        assertTrue(ex.message!!.contains(">= 48 bytes"))
    }

    @Test
    fun `expired token should be rejected`() {
        val p = props(ttlSeconds = 60) // 1 min de vida
        val jwt = JwtService(p)
        val past = Instant.now().minusSeconds(3600) // bem no passado
        val t = buildTokenAt(past, p, UUID.randomUUID(), "a@b.com") // exp = past + 60s -> já vencido
        assertFalse(jwt.isValid(t), "Expired token must be invalid")
    }

    fun strongSecretB64(bytes: Int = 64): String {
        val buf = ByteArray(bytes)
        SecureRandom().nextBytes(buf)
        return getEncoder().encodeToString(buf)
    }

    private fun signKeyFrom(secretB64: String): SecretKey =
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretB64))

    private fun buildTokenAt(at: Instant, props: JwtProps, userId: UUID, email: String): String {
        val exp = at.plusSeconds(props.ttlSeconds)
        return Jwts.builder()
            .id(UUID.randomUUID().toString())                 // jti
            .subject(userId.toString())                       // sub = UUID
            .issuer(props.issuer)                             // iss
            .audience().add(props.audience).and()             // aud
            .issuedAt(Date.from(at))                          // iat
            .notBefore(Date.from(at))                         // nbf = iat
            .expiration(Date.from(exp))                       // exp
            .claim("email", email)
            .claim("v", props.version)
            .signWith(signKeyFrom(props.secretB64), Jwts.SIG.HS384)
            .compact()
    }

}
