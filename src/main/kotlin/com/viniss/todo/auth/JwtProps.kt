package com.viniss.todo.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Base64

@ConfigurationProperties(prefix = "jwt")
data class JwtProps(
    val issuer: String = "tickr-api",
    val audience: String = "tickr-web",
    val secretB64: String,
    val ttlSeconds: Long = 900,
    val clockSkewSeconds: Long = 60,
    val version: Int = 1,
    val jwksUri: String? = null,
    val acceptRS256: Boolean = false,
    val rsaPrivateKeyPem: String? = null,
    val rsaKeyId: String? = null,
    val jwksCacheTtlSeconds: Long = 300,
    val jwksCacheRefreshSeconds: Long = 30
) {
    /** Decodifica o HMAC (aceita Base64 URL-safe) e valida tamanho (HS384 >= 48 bytes). */
    val hmacSecretBytes: ByteArray by lazy {
        val bytes = try {
            Base64.getUrlDecoder().decode(secretB64)
        } catch (_: IllegalArgumentException) {
            Base64.getDecoder().decode(secretB64)
        }
        require(bytes.size >= 48) { "JWT HS384 secret too short: need >= 48 bytes" }
        bytes
    }
}
