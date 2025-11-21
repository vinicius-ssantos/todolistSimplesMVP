package com.viniss.todo.auth

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Service responsible for refresh token lifecycle management.
 *
 * Refactored to follow Single Responsibility Principle (SRP):
 * - Only handles refresh token CRUD operations
 * - Scheduled cleanup moved to RefreshTokenMaintenanceService
 *
 * Benefits of refactoring:
 * - Easier to test (no scheduled tasks)
 * - Clear single responsibility (token lifecycle management)
 * - Better separation of concerns
 */
@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: AppUserRepository
) {
    private val logger = LoggerFactory.getLogger(RefreshTokenService::class.java)

    companion object {
        private const val REFRESH_TOKEN_VALIDITY_DAYS = 30L
    }

    /**
     * Creates a new refresh token for a user.
     *
     * @param userId The ID of the user
     * @return The generated refresh token string
     */
    @Transactional
    fun createRefreshToken(userId: UUID): String {
        val token = UUID.randomUUID().toString()
        val expiresAt = Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS)

        val refreshToken = RefreshTokenEntity(
            userId = userId,
            token = token,
            expiresAt = expiresAt
        )

        refreshTokenRepository.save(refreshToken)
        logger.info("Created refresh token for user: {}", userId)

        return token
    }

    /**
     * Validates a refresh token and returns the associated user ID.
     *
     * If the token is expired, it is automatically deleted.
     *
     * @param token The refresh token to validate
     * @return The user ID if valid, null otherwise
     */
    @Transactional
    fun validateAndGetUserId(token: String): UUID? {
        val refreshToken = refreshTokenRepository.findByToken(token)
            ?: run {
                logger.warn("Refresh token not found")
                return null
            }

        if (Instant.now().isAfter(refreshToken.expiresAt)) {
            logger.warn("Refresh token expired for user: {}", refreshToken.userId)
            refreshTokenRepository.delete(refreshToken)
            return null
        }

        return refreshToken.userId
    }

    /**
     * Revokes (deletes) a specific refresh token.
     *
     * @param token The refresh token to revoke
     */
    @Transactional
    fun revokeToken(token: String) {
        refreshTokenRepository.findByToken(token)?.let {
            refreshTokenRepository.delete(it)
            logger.info("Revoked refresh token for user: {}", it.userId)
        }
    }

    /**
     * Revokes all refresh tokens for a specific user.
     * Useful for security operations like password change or logout from all devices.
     *
     * @param userId The ID of the user
     * @return Number of tokens revoked
     */
    @Transactional
    fun revokeAllUserTokens(userId: UUID): Int {
        val deletedCount = refreshTokenRepository.deleteAllByUserId(userId)
        logger.info("Revoked {} refresh tokens for user: {}", deletedCount, userId)
        return deletedCount
    }

    /**
     * Checks if a refresh token exists and is valid.
     *
     * @param token The refresh token to check
     * @return true if the token exists and is not expired
     */
    @Transactional(readOnly = true)
    fun isValidToken(token: String): Boolean {
        val refreshToken = refreshTokenRepository.findByToken(token) ?: return false
        return Instant.now().isBefore(refreshToken.expiresAt)
    }

    /**
     * Gets the number of active refresh tokens for a user.
     *
     * @param userId The ID of the user
     * @return Number of active (non-expired) refresh tokens
     */
    @Transactional(readOnly = true)
    fun getActiveTokenCount(userId: UUID): Long {
        return refreshTokenRepository.countByUserIdAndExpiresAtAfter(userId, Instant.now())
    }
}
