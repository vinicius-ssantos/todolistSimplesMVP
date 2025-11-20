package com.viniss.todo.auth

import com.viniss.todo.auth.token.JwtTokenExtractor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Service responsible for managing JWT token blacklist.
 *
 * Refactored to follow Single Responsibility Principle (SRP):
 * - Only handles blacklist business logic
 * - Delegates JWT parsing to JwtTokenExtractor (separation of concerns)
 * - Scheduled cleanup moved to TokenBlacklistMaintenanceService
 *
 * Benefits of refactoring:
 * - Easier to test (no scheduled tasks, no parsing logic)
 * - Clear single responsibility
 * - Reusable JWT parsing logic
 */
@Service
class TokenBlacklistService(
    private val blacklistedTokenRepository: BlacklistedTokenRepository,
    private val jwtTokenExtractor: JwtTokenExtractor
) {
    private val logger = LoggerFactory.getLogger(TokenBlacklistService::class.java)

    /**
     * Adds a JWT token to the blacklist.
     *
     * @param token The JWT token to blacklist
     * @param userId The ID of the user who owns the token
     * @param reason Optional reason for blacklisting (e.g., "logout", "security")
     */
    @Transactional
    fun blacklistToken(token: String, userId: UUID, reason: String? = null) {
        val jti = jwtTokenExtractor.extractJti(token) ?: run {
            logger.warn("Cannot blacklist token: missing JTI")
            return
        }

        val expiresAt = jwtTokenExtractor.extractExpiration(token)

        val blacklistedToken = BlacklistedTokenEntity(
            tokenJti = jti,
            userId = userId,
            expiresAt = expiresAt,
            reason = reason
        )

        blacklistedTokenRepository.save(blacklistedToken)
        logger.info("Blacklisted token (JTI: {}) for user: {}, reason: {}", jti, userId, reason)
    }

    /**
     * Checks if a JWT token is blacklisted.
     *
     * @param token The JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    fun isBlacklisted(token: String): Boolean {
        val jti = jwtTokenExtractor.extractJti(token) ?: return false
        return blacklistedTokenRepository.existsByTokenJti(jti)
    }

    /**
     * Blacklists all tokens for a specific user.
     * Useful for account security operations (e.g., password change, account compromise).
     *
     * @param userId The ID of the user
     * @param reason Optional reason for blacklisting
     */
    @Transactional
    fun blacklistAllUserTokens(userId: UUID, reason: String? = null) {
        // This would require tracking all active tokens per user
        // For now, log the operation - implementation depends on requirements
        logger.info("Request to blacklist all tokens for user: {}, reason: {}", userId, reason)
        // TODO: Implement if needed - requires tracking active tokens per user
    }
}
