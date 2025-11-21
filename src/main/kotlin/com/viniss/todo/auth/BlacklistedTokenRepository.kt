package com.viniss.todo.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface BlacklistedTokenRepository : JpaRepository<BlacklistedTokenEntity, UUID> {
    fun existsByTokenJti(tokenJti: String): Boolean

    fun findByUserId(userId: UUID): List<BlacklistedTokenEntity>

    /**
     * Deletes all expired blacklisted tokens.
     * @return Number of tokens deleted
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM BlacklistedTokenEntity bt WHERE bt.expiresAt < :now")
    fun deleteExpiredTokens(now: Instant): Int
}
