package com.viniss.todo.auth.service

import com.viniss.todo.auth.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service responsible for refreshing access tokens.
 *
 * Follows Single Responsibility Principle (SRP):
 * - Only handles access token refresh logic
 * - Separated from registration, login, and logout operations
 *
 * Dependencies reduced from 7 (in AuthService) to 3:
 * - AppUserRepository
 * - TokenService
 * - RefreshTokenService
 *
 * Benefits:
 * - Clear single responsibility (token refresh only)
 * - Easier to test (minimal dependencies)
 * - Easier to extend (e.g., token rotation policies, refresh token families)
 * - Better security (focused on token lifecycle)
 * - Better maintainability
 */
@Service
class AccessTokenRefreshService(
    private val userRepository: AppUserRepository,
    private val tokenService: TokenService,
    private val refreshTokenService: RefreshTokenService
) {

    private val logger = LoggerFactory.getLogger(AccessTokenRefreshService::class.java)

    /**
     * Refreshes an access token using a valid refresh token.
     *
     * Security features:
     * - Validates refresh token
     * - Generates new access token
     * - Generates new refresh token (token rotation)
     * - Revokes old refresh token
     *
     * @param refreshToken The current refresh token
     * @return AuthResponseWithRefresh containing new access token and refresh token
     * @throws IllegalStateException if refresh token is invalid or user not found
     */
    fun refreshAccessToken(refreshToken: String): AuthResponseWithRefresh {
        logger.debug("Access token refresh attempt")

        // Validate refresh token and get user ID
        val userId = refreshTokenService.validateAndGetUserId(refreshToken)
            ?: run {
                logger.warn("Invalid or expired refresh token")
                error("Invalid or expired refresh token")
            }

        // Find user
        val user = userRepository.findById(userId)
            .orElseThrow {
                logger.error("User not found for valid refresh token: {}", userId)
                error("User not found")
            }

        // Generate new tokens
        val newAccessToken = tokenService.generateToken(email = user.email, userId = user.id)
        val newRefreshToken = refreshTokenService.createRefreshToken(user.id)

        // Revoke old refresh token (token rotation for better security)
        refreshTokenService.revokeToken(refreshToken)

        logger.info("Access token refreshed for user: {} (id: {})", user.email, userId)
        logger.debug("Old refresh token revoked, new tokens generated")

        return AuthResponseWithRefresh(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken
        )
    }
}
