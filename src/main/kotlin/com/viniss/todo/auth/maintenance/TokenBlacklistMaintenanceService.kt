package com.viniss.todo.auth.maintenance

import com.viniss.todo.auth.BlacklistedTokenRepository
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service responsible for scheduled maintenance of blacklisted tokens.
 *
 * Follows Single Responsibility Principle (SRP):
 * - Only handles scheduled cleanup tasks
 * - Separated from business logic (TokenBlacklistService)
 * - Separated from parsing logic (JwtTokenExtractor)
 *
 * This service runs background tasks to keep the blacklist database clean.
 */
@Service
class TokenBlacklistMaintenanceService(
    private val blacklistedTokenRepository: BlacklistedTokenRepository,
    private val entityManager: EntityManager
) {

    private val logger = LoggerFactory.getLogger(TokenBlacklistMaintenanceService::class.java)

    /**
     * Cleanup expired blacklisted tokens every day at 4 AM.
     *
     * Tokens are automatically removed from the blacklist once they expire,
     * since expired tokens cannot be used anyway. This prevents the blacklist
     * from growing indefinitely.
     *
     * Schedule: Daily at 04:00 (cron: "0 0 4 * * ?")
     */
    @Scheduled(cron = "0 0 4 * * ?")
    @Transactional
    fun cleanupExpiredTokens() {
        val now = Instant.now()
        logger.info("Starting cleanup of expired blacklisted tokens at {}", now)

        try {
            entityManager.flush() // Ensure pending changes are persisted before delete
            val deletedCount = blacklistedTokenRepository.deleteExpiredTokens(now)
            logger.info("Successfully cleaned up {} expired blacklisted tokens", deletedCount)
        } catch (e: Exception) {
            logger.error("Error during blacklisted tokens cleanup", e)
        }
    }

    /**
     * Manual cleanup trigger for testing or administrative purposes.
     *
     * @return Number of tokens deleted
     */
    @Transactional
    fun cleanupExpiredTokensNow(): Int {
        val now = Instant.now()
        logger.info("Manual cleanup triggered for expired blacklisted tokens")

        return try {
            entityManager.flush() // Ensure pending changes are persisted before delete
            val deletedCount = blacklistedTokenRepository.deleteExpiredTokens(now)
            logger.info("Manual cleanup completed: {} tokens deleted", deletedCount)
            deletedCount
        } catch (e: Exception) {
            logger.error("Error during manual cleanup of blacklisted tokens", e)
            0
        }
    }
}
