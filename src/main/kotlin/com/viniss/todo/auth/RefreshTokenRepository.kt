package com.viniss.todo.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByToken(token: String): RefreshTokenEntity?
    fun findByUserId(userId: UUID): List<RefreshTokenEntity>

    /**
     * Deletes all refresh tokens for a specific user.
     * @return Number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.userId = :userId")
    fun deleteAllByUserId(userId: UUID): Long

    /**
     * Deletes all expired refresh tokens.
     * @return Number of tokens deleted
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :now")
    fun deleteExpiredTokens(now: Instant): Long

    /**
     * Counts active (non-expired) refresh tokens for a user.
     */
    fun countByUserIdAndExpiresAtAfter(userId: UUID, now: Instant): Long

    /**
     * Counts expired refresh tokens.
     */
    fun countByExpiresAtBefore(now: Instant): Long
}
