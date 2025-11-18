package com.viniss.todo.auth

import org.springframework.data.jpa.repository.JpaRepository
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
    @Query(
        """
        DELETE FROM PasswordHistoryEntity ph
        WHERE ph.userId = :userId
        AND ph.id NOT IN (
            SELECT ph2.id FROM PasswordHistoryEntity ph2
            WHERE ph2.userId = :userId
            ORDER BY ph2.createdAt DESC
            LIMIT :keepCount
        )
        """
    )
    fun deleteOldEntries(userId: UUID, keepCount: Int)
}
