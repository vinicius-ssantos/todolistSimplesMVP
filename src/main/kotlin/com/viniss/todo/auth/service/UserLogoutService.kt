package com.viniss.todo.auth.service

import com.viniss.todo.auth.RefreshTokenService
import com.viniss.todo.auth.TokenBlacklistService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service responsible for user logout operations.
 *
 * Follows Single Responsibility Principle (SRP):
 * - Only handles logout logic
 * - Separated from registration, login, and token refresh operations
 *
 * Dependencies reduced from 7 (in AuthService) to 2:
 * - RefreshTokenService (to revoke refresh tokens)
 * - TokenBlacklistService (to blacklist access tokens)
 *
 * Benefits:
 * - Clear single responsibility (logout only)
 * - Easiest to test (minimal dependencies)
 * - Easier to extend (e.g., logout from all devices, logout notifications)
 * - Better security (focused on token revocation)
 * - Better maintainability
 */
@Service
class UserLogoutService(
    private val refreshTokenService: RefreshTokenService,
    private val tokenBlacklistService: TokenBlacklistService
) {

    private val logger = LoggerFactory.getLogger(UserLogoutService::class.java)

    /**
     * Logs out a user by revoking tokens.
     *
     * Security features:
     * - Blacklists current access token (prevents reuse)
     * - Revokes all refresh tokens for the user
     *
     * @param userId The ID of the user logging out
     * @param accessToken The current access token to blacklist
     */
    fun logout(userId: UUID, accessToken: String) {
        logger.info("Logout initiated for user: {}", userId)

        // Blacklist current access token to prevent reuse
        tokenBlacklistService.blacklistToken(
            token = accessToken,
            userId = userId,
            reason = "logout"
        )
        logger.debug("Access token blacklisted for user: {}", userId)

        // Revoke all refresh tokens for user (logout from all devices)
        val revokedCount = refreshTokenService.revokeAllUserTokens(userId)
        logger.info("User logged out successfully: {} ({} refresh tokens revoked)", userId, revokedCount)
    }

    /**
     * Logs out a user from all devices.
     * Alias for logout() since it already revokes all refresh tokens.
     *
     * @param userId The ID of the user logging out
     * @param accessToken The current access token to blacklist
     */
    fun logoutAllDevices(userId: UUID, accessToken: String) {
        logger.info("Logout from all devices initiated for user: {}", userId)
        logout(userId, accessToken)
    }
}
