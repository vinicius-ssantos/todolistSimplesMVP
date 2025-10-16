package com.viniss.todo.auth

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.util.*

data class AuthPrincipal(
    val userId: UUID,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val audience: List<String>,
    val jwtId: String?
)

class TokenService(
    private val props: JwtProps,
    private val nowProvider: () -> Instant = { Instant.now() },
) {
    // --- ASSINAR (HS384 hoje) ---
    fun signHS384(
        userId: UUID,
        ttlSeconds: Long = 3600,
        extraClaims: Map<String, Any> = emptyMap(),
        kid: String? = null // futuro: já permite setar um kid no header
    ): String {
        val now = nowProvider()
        val claims = JWTClaimsSet.Builder()
            .issuer(props.issuer)
            .audience(props.audience)
            .subject(userId.toString())     // sub = UUID
            .issueTime(Date.from(now))
            .notBeforeTime(Date.from(now.minusSeconds(props.clockSkewSeconds)))
            .expirationTime(Date.from(now.plusSeconds(ttlSeconds)))
            .jwtID(UUID.randomUUID().toString())
            .apply {
                extraClaims.forEach { (k, v) -> claim(k, v) }
            }
            .build()

        val header = JWSHeader.Builder(JWSAlgorithm.HS384)
            .type(JOSEObjectType.JWT)
            .apply { if (kid != null) keyID(kid) }
            .build()

        val jwt = SignedJWT(header, claims)
        jwt.sign(MACSigner(props.hmacSecretBytes))
        return jwt.serialize()
    }

    // --- VALIDAR & PARSE ---
    fun parseAndValidate(token: String): AuthPrincipal {
        val jwt = try {
            SignedJWT.parse(token)
        } catch (e: Exception) {
            throw InvalidTokenException("Malformed JWT")
        }

        val alg = jwt.header.algorithm
        when (alg) {
            JWSAlgorithm.HS384 -> verifyHS(jwt)
            // Futuro: quando aceitar RS256, verificar via JWKS por kid:
            JWSAlgorithm.RS256 -> if (props.acceptRS256) {
                // placeholder: implementar RSASSAVerifier com JWKS (RemoteJWKSet) no futuro
                throw InvalidTokenException("RS256 not yet enabled")
            } else {
                throw InvalidTokenException("Unexpected alg: $alg")
            }
            else -> throw InvalidTokenException("Unsupported alg: $alg")
        }

        val claims = jwt.jwtClaimsSet ?: throw InvalidTokenException("Missing claims")
        validateClaims(claims)

        val sub = claims.subject ?: throw InvalidTokenException("Missing sub")
        val userId = try { UUID.fromString(sub) } catch (_: IllegalArgumentException) {
            throw InvalidTokenException("Invalid sub (UUID required)")
        }

        return AuthPrincipal(
            userId = userId,
            issuedAt = claims.issueTime.toInstant(),
            expiresAt = claims.expirationTime.toInstant(),
            audience = claims.audience,
            jwtId = claims.jwtid
        )
    }

    private fun verifyHS(jwt: SignedJWT) {
        val ok = jwt.verify(MACVerifier(props.hmacSecretBytes))
        if (!ok) throw InvalidTokenException("Invalid signature")
    }

    private fun validateClaims(claims: JWTClaimsSet) {
        val now = nowProvider()
        val skew = props.clockSkewSeconds

        val iss = claims.issuer ?: throw InvalidTokenException("Missing iss")
        if (iss != props.issuer) throw InvalidTokenException("Invalid issuer")

        val aud = claims.audience ?: emptyList()
        if (props.audience !in aud) throw InvalidTokenException("Invalid audience")

        val exp = claims.expirationTime?.toInstant() ?: throw InvalidTokenException("Missing exp")
        if (now.isAfter(exp.plusSeconds(skew))) throw InvalidTokenException("Token expired")

        val nbf = claims.notBeforeTime?.toInstant()
        if (nbf != null && now.isBefore(nbf.minusSeconds(skew))) throw InvalidTokenException("Token not yet valid")

        val iat = claims.issueTime?.toInstant() ?: throw InvalidTokenException("Missing iat")
        if (iat.isAfter(now.plusSeconds(skew))) throw InvalidTokenException("iat in the future")
    }
}