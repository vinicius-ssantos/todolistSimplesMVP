package com.viniss.todo.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface BlacklistedTokenRepository : JpaRepository<BlacklistedTokenEntity, UUID> {
    fun existsByTokenJti(tokenJti: String): Boolean

    @Modifying
    @Query("DELETE FROM BlacklistedTokenEntity bt WHERE bt.expiresAt < :now")
    fun deleteExpiredTokens(now: Instant)
}
