package com.viniss.todo.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface SharedListRepository : JpaRepository<SharedListEntity, UUID> {

    /**
     * Find all shares for a specific list.
     */
    fun findByListId(listId: UUID): List<SharedListEntity>

    /**
     * Find all lists shared with a specific user.
     */
    fun findBySharedWithUserId(sharedWithUserId: UUID): List<SharedListEntity>

    /**
     * Find a specific share.
     */
    fun findByListIdAndSharedWithUserId(listId: UUID, sharedWithUserId: UUID): SharedListEntity?

    /**
     * Check if a list is shared with a specific user.
     */
    fun existsByListIdAndSharedWithUserId(listId: UUID, sharedWithUserId: UUID): Boolean

    /**
     * Find all lists that a user has access to (either owned or shared).
     */
    @Query("""
        SELECT sl FROM SharedListEntity sl
        WHERE sl.listId = :listId
        AND sl.sharedWithUserId = :userId
    """)
    fun findSharedListForUser(listId: UUID, userId: UUID): SharedListEntity?

    /**
     * Delete all shares for a specific list.
     */
    fun deleteByListId(listId: UUID)

    /**
     * Delete a specific share.
     */
    fun deleteByListIdAndSharedWithUserId(listId: UUID, sharedWithUserId: UUID)
}
