package com.viniss.todo.auth.maintenance

import com.viniss.todo.auth.RefreshTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Service responsible for scheduled maintenance of refresh tokens.
 *
 * Follows Single Responsibility Principle (SRP):
 * - Only handles scheduled cleanup tasks
 * - Separated from business logic (RefreshTokenService)
 *
 * This service runs background tasks to prevent the refresh token table
 * from growing indefinitely.
 */
@Service
class RefreshTokenMaintenanceService(
    private val refreshTokenRepository: RefreshTokenRepository
) {

    private val logger = LoggerFactory.getLogger(RefreshTokenMaintenanceService::class.java)

    /**
     * Cleanup expired refresh tokens every day at 3 AM.
     *
     * Expired refresh tokens are automatically removed to:
     * - Keep the database size manageable
     * - Improve query performance
     * - Remove stale data
     *
     * Schedule: Daily at 03:00 (cron: "0 0 3 * * ?")
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    fun cleanupExpiredTokens() {
        val now = Instant.now()
        logger.info("Starting cleanup of expired refresh tokens at {}", now)

        try {
            val deletedCount = refreshTokenRepository.deleteExpiredTokens(now)
            logger.info("Successfully cleaned up {} expired refresh tokens", deletedCount)
        } catch (e: Exception) {
            logger.error("Error during refresh tokens cleanup", e)
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
        logger.info("Manual cleanup triggered for expired refresh tokens")

        return try {
            val deletedCount = refreshTokenRepository.deleteExpiredTokens(now)
            logger.info("Manual cleanup completed: {} tokens deleted", deletedCount)
            deletedCount
        } catch (e: Exception) {
            logger.error("Error during manual cleanup of refresh tokens", e)
            0
        }
    }

    /**
     * Returns statistics about refresh tokens.
     * Useful for monitoring and observability.
     *
     * @return Map containing token statistics
     */
    @Transactional(readOnly = true)
    fun getTokenStatistics(): Map<String, Long> {
        return try {
            val totalCount = refreshTokenRepository.count()
            val expiredCount = refreshTokenRepository.countByExpiresAtBefore(Instant.now())
            val activeCount = totalCount - expiredCount

            mapOf(
                "total" to totalCount,
                "active" to activeCount,
                "expired" to expiredCount
            )
        } catch (e: Exception) {
            logger.error("Error getting token statistics", e)
            emptyMap()
        }
    }
}
