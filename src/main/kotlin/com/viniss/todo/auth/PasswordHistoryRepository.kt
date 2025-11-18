package com.viniss.todo.auth

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.*

interface PasswordHistoryRepository : JpaRepository<PasswordHistoryEntity, UUID> {

    /**
     * Find the most recent N password hashes for a user.
     *
     * @param userId The user ID
     * @param limit Maximum number of records to return
     * @return List of password history entries ordered by creation date (newest first)
     */
    @Query(
        """
        SELECT ph FROM PasswordHistoryEntity ph
        WHERE ph.userId = :userId
        ORDER BY ph.createdAt DESC
        LIMIT :limit
        """
    )
    fun findRecentByUserId(userId: UUID, limit: Int): List<PasswordHistoryEntity>

    /**
     * Delete old password history entries beyond the retention limit.
     *
     * @param userId The user ID
     * @param keepCount Number of most recent entries to keep
     */
    @Modifying
    @Query(
        value = """
        DELETE FROM password_history
        WHERE user_id = :userId
        AND id NOT IN (
            SELECT id FROM (
                SELECT id FROM password_history
                WHERE user_id = :userId
                ORDER BY created_at DESC
                LIMIT :keepCount
            ) AS keep_ids
        )
        """,
        nativeQuery = true
    )
    fun deleteOldEntries(userId: UUID, keepCount: Int)
}
