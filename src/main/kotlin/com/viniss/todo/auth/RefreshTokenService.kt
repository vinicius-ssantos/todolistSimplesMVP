package com.viniss.todo.auth

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: AppUserRepository
) {
    private val logger = LoggerFactory.getLogger(RefreshTokenService::class.java)

    companion object {
        private const val REFRESH_TOKEN_VALIDITY_DAYS = 30L
    }

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
        logger.info("Created refresh token for user: $userId")

        return token
    }

    @Transactional
    fun validateAndGetUserId(token: String): UUID? {
        val refreshToken = refreshTokenRepository.findByToken(token)
            ?: run {
                logger.warn("Refresh token not found")
                return null
            }

        if (Instant.now().isAfter(refreshToken.expiresAt)) {
            logger.warn("Refresh token expired for user: ${refreshToken.userId}")
            refreshTokenRepository.delete(refreshToken)
            return null
        }

        return refreshToken.userId
    }

    @Transactional
    fun revokeToken(token: String) {
        refreshTokenRepository.findByToken(token)?.let {
            refreshTokenRepository.delete(it)
            logger.info("Revoked refresh token for user: ${it.userId}")
        }
    }

    @Transactional
    fun revokeAllUserTokens(userId: UUID) {
        refreshTokenRepository.deleteAllByUserId(userId)
        logger.info("Revoked all refresh tokens for user: $userId")
    }

    /**
     * Cleanup expired tokens every day at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    fun cleanupExpiredTokens() {
        val deletedCount = refreshTokenRepository.deleteExpiredTokens(Instant.now())
        logger.info("Cleaned up expired refresh tokens")
    }
}
