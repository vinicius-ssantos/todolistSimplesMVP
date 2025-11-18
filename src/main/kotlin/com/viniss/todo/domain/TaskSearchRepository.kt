package com.viniss.todo.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

/**
 * Repository for full-text search on tasks.
 */
interface TaskSearchRepository : JpaRepository<TaskEntity, UUID> {

    /**
     * Full-text search across task titles and notes.
     * Uses PostgreSQL's full-text search capabilities.
     *
     * @param userId The user ID to filter tasks
     * @param searchQuery The search query (can be multiple words)
     * @return List of tasks matching the search query, ordered by relevance
     */
    @Query("""
        SELECT t FROM TaskEntity t
        WHERE t.userId = :userId
        AND to_tsvector('english', COALESCE(t.title, '') || ' ' || COALESCE(t.notes, ''))
            @@ plainto_tsquery('english', :searchQuery)
        ORDER BY ts_rank(
            to_tsvector('english', COALESCE(t.title, '') || ' ' || COALESCE(t.notes, '')),
            plainto_tsquery('english', :searchQuery)
        ) DESC
    """, nativeQuery = true)
    fun fullTextSearch(
        @Param("userId") userId: UUID,
        @Param("searchQuery") searchQuery: String
    ): List<TaskEntity>

    /**
     * Search tasks by tag.
     */
    @Query("""
        SELECT DISTINCT t FROM TaskEntity t
        JOIN t.tags tag
        WHERE t.userId = :userId
        AND tag.name = :tagName
    """)
    fun findByUserIdAndTagName(userId: UUID, tagName: String): List<TaskEntity>

    /**
     * Find recurring tasks for a user.
     */
    fun findByUserIdAndIsRecurringTrue(userId: UUID): List<TaskEntity>

    /**
     * Find child tasks of a recurring parent.
     */
    fun findByParentRecurringTaskId(parentId: UUID): List<TaskEntity>
}
