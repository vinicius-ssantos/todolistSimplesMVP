package com.viniss.todo.auth.token

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

/**
 * Component responsible for extracting information from JWT tokens.
 *
 * Follows Single Responsibility Principle:
 * - Only handles JWT parsing and extraction logic
 * - Separated from business logic and scheduled tasks
 *
 * This is a low-level utility that can be reused across different services.
 */
@Component
class JwtTokenExtractor {

    private val logger = LoggerFactory.getLogger(JwtTokenExtractor::class.java)

    /**
     * Extracts the JTI (JWT ID) claim from a JWT token.
     *
     * This method performs lightweight parsing without full validation,
     * useful for blacklist checks before expensive validation.
     *
     * @param token The JWT token string
     * @return The JTI value, or null if extraction fails
     */
    fun extractJti(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                logger.warn("Invalid JWT format: expected 3 parts, got ${parts.size}")
                return null
            }

            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            // Extract JTI using regex (lightweight parsing)
            val jtiMatch = Regex("\"jti\"\\s*:\\s*\"([^\"]+)\"").find(payload)
            jtiMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.error("Error extracting JTI from token", e)
            null
        }
    }

    /**
     * Extracts the expiration time from a JWT token.
     *
     * This method performs lightweight parsing without full validation.
     *
     * @param token The JWT token string
     * @return The expiration instant, or a default value (15 minutes from now) if extraction fails
     */
    fun extractExpiration(token: String): Instant {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                return defaultExpiration()
            }

            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            // Extract exp claim using regex
            val expMatch = Regex("\"exp\"\\s*:\\s*(\\d+)").find(payload)
            val expSeconds = expMatch?.groupValues?.get(1)?.toLongOrNull()

            if (expSeconds != null) {
                Instant.ofEpochSecond(expSeconds)
            } else {
                defaultExpiration()
            }
        } catch (e: Exception) {
            logger.error("Error extracting expiration from token", e)
            defaultExpiration()
        }
    }

    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token The JWT token string
     * @return The user ID as UUID, or null if extraction fails
     */
    fun extractUserId(token: String): UUID? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                return null
            }

            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            // Extract sub (subject) claim which typically contains user ID
            val subMatch = Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"").find(payload)
            val subValue = subMatch?.groupValues?.get(1)

            subValue?.let { UUID.fromString(it) }
        } catch (e: Exception) {
            logger.error("Error extracting user ID from token", e)
            null
        }
    }

    /**
     * Returns the default expiration time (15 minutes from now).
     * Used as fallback when expiration cannot be extracted.
     */
    private fun defaultExpiration(): Instant {
        return Instant.now().plusSeconds(900) // 15 minutes
    }

    /**
     * Validates the basic structure of a JWT token.
     *
     * @param token The JWT token string
     * @return true if the token has valid JWT structure (3 parts separated by dots)
     */
    fun hasValidStructure(token: String): Boolean {
        return token.split(".").size == 3
    }
}
