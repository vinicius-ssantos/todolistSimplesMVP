package com.viniss.todo.auth

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.util.JSONObjectUtils
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jose.util.Resource
import java.net.URI
import java.net.URL
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * TokenService baseado em RS256, com validacao via JWKS remoto (Nimbus).
 * - Assina usando chave RSA privada (PKCS#8) e injeta `kid` no header.
 * - Valida assinatura contra JWKS remoto com cache e tentativa de atualizacao.
 * - Garante validacao de claims padrao (iss, aud, nbf, iat, exp).
 */
class NimbusRsaTokenService @JvmOverloads constructor(
    private val props: JwtProps,
    private val nowProvider: () -> Instant = { Instant.now() },
    jwksFetcher: (() -> JWKSet)? = null
) : TokenService {

    private val keyId: String = props.rsaKeyId?.takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("jwt.rsaKeyId is required when acceptRS256=true")

    private val jwksUri: URL = props.jwksUri?.let { URI(it).toURL() }
        ?: throw IllegalArgumentException("jwt.jwksUri is required when acceptRS256=true")

    private val signer: JWSSigner
    private val jwksFetcher: () -> JWKSet
    private val jwksCache = AtomicReference<CachedJwkSet?>()
    private val cacheLock = Any()
    private val refreshMarginSeconds: Long =
        props.jwksCacheRefreshSeconds.coerceAtMost(props.jwksCacheTtlSeconds)

    private data class CachedJwkSet(val jwkSet: JWKSet, val expiresAt: Instant)

    init {
        val privateKeyPem = props.rsaPrivateKeyPem?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("jwt.rsaPrivateKeyPem (PKCS#8) is required when acceptRS256=true")
        val privateKey = parsePrivateKey(privateKeyPem)
        signer = RSASSASigner(privateKey)

        this.jwksFetcher = jwksFetcher ?: { fetchRemoteJwkSet() }
    }

    override fun generateToken(userId: UUID, email: String): String {
        val now = nowProvider()
        val exp = now.plusSeconds(props.ttlSeconds)
        val claims = JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .subject(userId.toString())
            .issuer(props.issuer)
            .audience(props.audience)
            .issueTime(Date.from(now))
            .notBeforeTime(Date.from(now))
            .expirationTime(Date.from(exp))
            .claim("email", email)
            .claim("v", props.version)
            .build()

        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .keyID(keyId)
            .build()

        val signed = SignedJWT(header, claims)
        signed.sign(signer)
        return signed.serialize()
    }

    private fun parseClaims(token: String): JWTClaimsSet {
        val jwt = try {
            SignedJWT.parse(token)
        } catch (ex: Exception) {
            throw InvalidTokenException("Malformed JWT")
        }

        val header = jwt.header
        if (header.algorithm != JWSAlgorithm.RS256) {
            throw InvalidTokenException("Unsupported alg: ${header.algorithm.name}")
        }

        val verifier = jwsVerifierFor(header)
        if (!jwt.verify(verifier)) {
            throw InvalidTokenException("Invalid signature")
        }

        val claims = jwt.jwtClaimsSet ?: throw InvalidTokenException("Missing claims")
        validateClaims(claims)
        return claims
    }

    override fun isValid(token: String): Boolean = runCatching { parseClaims(token) }.isSuccess

    override fun extractUserId(token: String): UUID =
        runCatching { UUID.fromString(parseClaims(token).subject) }
            .getOrElse { throw InvalidTokenException("sub is not a valid UUID") }

    override fun extractEmail(token: String): String =
        parseClaims(token).getStringClaim("email")

    private fun jwsVerifierFor(header: JWSHeader): JWSVerifier {
        val kid = header.keyID
        val rsaKey = resolveRsaKey(kid)
        return RSASSAVerifier(rsaKey)
    }

    private fun resolveRsaKey(kid: String?): RSAKey {
        val currentSet = currentJwkSet(forceRefresh = false)
        findRsaKey(currentSet, kid)?.let { return it }

        val refreshed = currentJwkSet(forceRefresh = true)
        return findRsaKey(refreshed, kid)
            ?: throw InvalidTokenException("RSA key not found for kid=$kid")
    }

    private fun findRsaKey(jwkSet: JWKSet, kid: String?): RSAKey? {
        val keys = jwkSet.keys.filterIsInstance<RSAKey>()
        if (keys.isEmpty()) return null
        return if (kid.isNullOrBlank()) {
            keys.firstOrNull()
        } else {
            keys.firstOrNull { it.keyID == kid }
        }
    }

    private fun currentJwkSet(forceRefresh: Boolean): JWKSet {
        val now = nowProvider()
        if (!forceRefresh) {
            val cached = jwksCache.get()?.takeIf { shouldUseCache(now, it) }
            if (cached != null) return cached.jwkSet
        }

        synchronized(cacheLock) {
            if (!forceRefresh) {
                val cached = jwksCache.get()?.takeIf { shouldUseCache(now, it) }
                if (cached != null) return cached.jwkSet
            }
            return try {
                val jwkSet = jwksFetcher.invoke()
                val expiresAt = now.plusSeconds(props.jwksCacheTtlSeconds)
                jwksCache.set(CachedJwkSet(jwkSet, expiresAt))
                jwkSet
            } catch (ex: Exception) {
                jwksCache.get()?.jwkSet
                    ?: throw InvalidTokenException("Unable to fetch JWKS: ${ex.message}")
            }
        }
    }

    private fun fetchRemoteJwkSet(): JWKSet {
        // TODO: add staging integration that hits real JWKS endpoint and validates RS256 flow end-to-end.
        val retriever = DefaultResourceRetriever(
            2000,
            2000,
            8 * 1024
        )
        val resource: Resource = retriever.retrieveResource(jwksUri)
        val jsonObject = JSONObjectUtils.parse(resource.content)
        return JWKSet.parse(jsonObject)
    }

    private fun shouldUseCache(now: Instant, cached: CachedJwkSet): Boolean {
        if (now.isAfter(cached.expiresAt)) return false
        if (refreshMarginSeconds <= 0) return true
        val refreshThreshold = cached.expiresAt.minusSeconds(refreshMarginSeconds)
        return now.isBefore(refreshThreshold)
    }

    private fun validateClaims(claims: JWTClaimsSet) {
        val now = nowProvider()
        val skew = props.clockSkewSeconds

        val issuer = claims.issuer ?: throw InvalidTokenException("Missing iss")
        if (issuer != props.issuer) throw InvalidTokenException("Invalid issuer")

        val audience = claims.audience ?: emptyList()
        if (props.audience !in audience) throw InvalidTokenException("Invalid audience")

        val exp = claims.expirationTime?.toInstant() ?: throw InvalidTokenException("Missing exp")
        if (now.isAfter(exp.plusSeconds(skew))) throw InvalidTokenException("Token expired")

        val nbf = claims.notBeforeTime?.toInstant()
        if (nbf != null && now.isBefore(nbf.minusSeconds(skew))) {
            throw InvalidTokenException("Token not yet valid")
        }

        val iat = claims.issueTime?.toInstant() ?: throw InvalidTokenException("Missing iat")
        if (iat.isAfter(now.plusSeconds(skew))) {
            throw InvalidTokenException("iat in the future")
        }
    }

    private fun parsePrivateKey(pem: String): RSAPrivateKey {
        val sanitized = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")

        val keyBytes = try {
            Base64.getDecoder().decode(sanitized)
        } catch (ex: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid RSA private key: ${ex.message}")
        }

        val keyFactory = KeyFactory.getInstance("RSA")
        return try {
            keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyBytes)) as RSAPrivateKey
        } catch (ex: Exception) {
            throw IllegalArgumentException("RSA private key must be in PKCS#8 format: ${ex.message}")
        }
    }
}
