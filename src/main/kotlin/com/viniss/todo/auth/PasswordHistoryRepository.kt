package com.viniss.todo.auth

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface PasswordHistoryRepository : JpaRepository<PasswordHistoryEntity, UUID> {

    /**
     * Find password history entries for a user ordered by creation date (newest first).
     *
     * @param userId The user ID
     * @param pageable Pagination information (use PageRequest.of(0, limit) to limit results)
     * @return List of password history entries ordered by creation date (newest first)
     */
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID, pageable: Pageable): List<PasswordHistoryEntity>

}
