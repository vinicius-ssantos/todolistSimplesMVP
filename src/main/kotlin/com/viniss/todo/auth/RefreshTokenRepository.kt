package com.viniss.todo.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.*

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByToken(token: String): RefreshTokenEntity?
    fun findByUserId(userId: UUID): List<RefreshTokenEntity>

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.userId = :userId")
    fun deleteAllByUserId(userId: UUID)

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :now")
    fun deleteExpiredTokens(now: Instant)
}
