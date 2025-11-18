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
        value = """
        SELECT * FROM password_history
        WHERE user_id = :userId
        ORDER BY created_at DESC
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findRecentByUserId(userId: UUID, limit: Int): List<PasswordHistoryEntity>

}
