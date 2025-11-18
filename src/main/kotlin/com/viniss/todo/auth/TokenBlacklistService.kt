package com.viniss.todo.auth

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class TokenBlacklistService(
    private val blacklistedTokenRepository: BlacklistedTokenRepository,
    private val tokenService: TokenService
) {
    private val logger = LoggerFactory.getLogger(TokenBlacklistService::class.java)

    @Transactional
    fun blacklistToken(token: String, userId: UUID, reason: String? = null) {
        val jti = extractJti(token) ?: run {
            logger.warn("Cannot blacklist token: missing JTI")
            return
        }

        // Extract expiration from token
        val expiresAt = try {
            val claims = Jwts.parser()
                .build()
                .parseUnsecuredClaims(token.substringAfter(".").substringBeforeLast("."))
                .payload
            claims.expiration?.toInstant() ?: Instant.now().plusSeconds(900)
        } catch (e: Exception) {
            Instant.now().plusSeconds(900) // Default 15 minutes
        }

        val blacklistedToken = BlacklistedTokenEntity(
            tokenJti = jti,
            userId = userId,
            expiresAt = expiresAt,
            reason = reason
        )

        blacklistedTokenRepository.save(blacklistedToken)
        logger.info("Blacklisted token for user: $userId, reason: $reason")
    }

    fun isBlacklisted(token: String): Boolean {
        val jti = extractJti(token) ?: return false
        return blacklistedTokenRepository.existsByTokenJti(jti)
    }

    private fun extractJti(token: String): String? {
        return try {
            // Extract JTI from token without full validation
            val parts = token.split(".")
            if (parts.size != 3) return null

            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            // Simple JSON parsing for JTI (you could use Jackson for more robust parsing)
            val jtiMatch = Regex("\"jti\"\\s*:\\s*\"([^\"]+)\"").find(payload)
            jtiMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.error("Error extracting JTI from token", e)
            null
        }
    }

    /**
     * Cleanup expired blacklisted tokens every day at 4 AM
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    fun cleanupExpiredTokens() {
        blacklistedTokenRepository.deleteExpiredTokens(Instant.now())
        logger.info("Cleaned up expired blacklisted tokens")
    }
}
