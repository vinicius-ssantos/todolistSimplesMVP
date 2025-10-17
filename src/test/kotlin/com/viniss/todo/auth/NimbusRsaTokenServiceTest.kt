package com.viniss.todo.auth

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class NimbusRsaTokenServiceTest {

    private val fixedNow: Instant = Instant.parse("2024-01-01T00:00:00Z")
    private val basePropsSecret: String = strongSecretB64()

    @Test
    fun `generate token with RS256 and validate`() {
        val keyPair = generateRsaKeyPair()
        val rsaJwk = RSAKey.Builder(keyPair.public as RSAPublicKey)
            .privateKey(keyPair.private as RSAPrivateKey)
            .keyID("kid-1")
            .build()

        val props = rsaProps(
            keyId = rsaJwk.keyID,
            privateKey = rsaJwk.toRSAPrivateKey(),
            jwksUri = "https://jwks.example/mock"
        )

        val service = NimbusRsaTokenService(
            props = props,
            nowProvider = { fixedNow },
            jwksFetcher = { JWKSet(rsaJwk.toPublicJWK()) }
        )

        val userId = UUID.randomUUID()
        val email = "rs@example.com"
        val token = service.generateToken(userId, email)

        assertTrue(service.isValid(token))
        assertEquals(userId, service.extractUserId(token))
        assertEquals(email, service.extractEmail(token))

        val parsed = SignedJWT.parse(token)
        assertEquals(JWSAlgorithm.RS256, parsed.header.algorithm)
        assertEquals("kid-1", parsed.header.keyID)
    }

    @Test
    fun `jwks refresh resolves rotated key`() {
        val primaryKeyPair = generateRsaKeyPair()
        val primaryJwk = RSAKey.Builder(primaryKeyPair.public as RSAPublicKey)
            .privateKey(primaryKeyPair.private as RSAPrivateKey)
            .keyID("kid-primary")
            .build()

        val rotatedKeyPair = generateRsaKeyPair()
        val rotatedJwk = RSAKey.Builder(rotatedKeyPair.public as RSAPublicKey)
            .privateKey(rotatedKeyPair.private as RSAPrivateKey)
            .keyID("kid-rotated")
            .build()

        val props = rsaProps(
            keyId = primaryJwk.keyID,
            privateKey = primaryJwk.toRSAPrivateKey(),
            jwksUri = "https://jwks.example/mock",
            cacheTtl = 60,
            cacheRefresh = 10
        )

        val jwkRef = AtomicReference(JWKSet(primaryJwk.toPublicJWK()))
        val service = NimbusRsaTokenService(
            props = props,
            nowProvider = { fixedNow },
            jwksFetcher = { jwkRef.get() }
        )

        val userId = UUID.randomUUID()
        val email = "user@example.com"
        val currentToken = service.generateToken(userId, email)

        assertTrue(service.isValid(currentToken))
        jwkRef.set(JWKSet(rotatedJwk.toPublicJWK()))

        val externalToken = signedToken(
            rsaKey = rotatedJwk,
            userId = UUID.randomUUID(),
            email = "rotated@example.com",
            issuedAt = fixedNow,
            ttlSeconds = props.ttlSeconds,
            issuer = props.issuer,
            audience = props.audience,
            version = props.version
        )

        assertTrue(service.isValid(externalToken))
        assertEquals("rotated@example.com", service.extractEmail(externalToken))
    }

    @Test
    fun `token signed with unknown key is rejected`() {
        val primaryPair = generateRsaKeyPair()
        val jwk = RSAKey.Builder(primaryPair.public as RSAPublicKey)
            .privateKey(primaryPair.private as RSAPrivateKey)
            .keyID("kid-1")
            .build()

        val props = rsaProps(
            keyId = jwk.keyID,
            privateKey = jwk.toRSAPrivateKey(),
            jwksUri = "https://jwks.example/mock"
        )

        val service = NimbusRsaTokenService(
            props = props,
            nowProvider = { fixedNow },
            jwksFetcher = { JWKSet(jwk.toPublicJWK()) }
        )

        // token signed with key not present in JWKS
        val unknownPair = generateRsaKeyPair()
        val unknownJwk = RSAKey.Builder(unknownPair.public as RSAPublicKey)
            .privateKey(unknownPair.private as RSAPrivateKey)
            .keyID("kid-unknown")
            .build()

        val forgedToken = signedToken(
            rsaKey = unknownJwk,
            userId = UUID.randomUUID(),
            email = "bad@example.com",
            issuedAt = fixedNow,
            ttlSeconds = props.ttlSeconds,
            issuer = props.issuer,
            audience = props.audience,
            version = props.version
        )

        assertFalse(service.isValid(forgedToken))
    }

    private fun rsaProps(
        keyId: String,
        privateKey: RSAPrivateKey,
        jwksUri: String,
        cacheTtl: Long = 120,
        cacheRefresh: Long = 30
    ): JwtProps =
        JwtProps(
            issuer = "tickr-api",
            audience = "tickr-web",
            secretB64 = basePropsSecret,
            ttlSeconds = 3600,
            clockSkewSeconds = 60,
            version = 1,
            jwksUri = jwksUri,
            acceptRS256 = true,
            rsaPrivateKeyPem = pkcs8Pem(privateKey),
            rsaKeyId = keyId,
            jwksCacheTtlSeconds = cacheTtl,
            jwksCacheRefreshSeconds = cacheRefresh
        )

    private fun signedToken(
        rsaKey: RSAKey,
        userId: UUID,
        email: String,
        issuedAt: Instant,
        ttlSeconds: Long,
        issuer: String,
        audience: String,
        version: Int
    ): String {
        val exp = issuedAt.plusSeconds(ttlSeconds)
        val claims = JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .subject(userId.toString())
            .issuer(issuer)
            .audience(audience)
            .issueTime(java.util.Date.from(issuedAt))
            .notBeforeTime(java.util.Date.from(issuedAt))
            .expirationTime(java.util.Date.from(exp))
            .claim("email", email)
            .claim("v", version)
            .build()

        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .keyID(rsaKey.keyID)
            .build()

        val jwt = SignedJWT(header, claims)
        jwt.sign(RSASSASigner(rsaKey))
        return jwt.serialize()
    }

    private fun pkcs8Pem(privateKey: RSAPrivateKey): String {
        val encoded = Base64.getEncoder().encodeToString(privateKey.encoded)
        return buildString {
            appendLine("-----BEGIN PRIVATE KEY-----")
            encoded.chunked(64).forEach { appendLine(it) }
            append("-----END PRIVATE KEY-----")
        }
    }

    private fun strongSecretB64(bytes: Int = 64): String {
        val buffer = ByteArray(bytes)
        java.security.SecureRandom().nextBytes(buffer)
        return Base64.getEncoder().encodeToString(buffer)
    }

    private fun generateRsaKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }
}
