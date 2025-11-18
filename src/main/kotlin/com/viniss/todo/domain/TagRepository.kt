package com.viniss.todo.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TagRepository : JpaRepository<TagEntity, UUID> {

    /**
     * Find all tags for a specific user.
     */
    fun findByUserId(userId: UUID): List<TagEntity>

    /**
     * Find a tag by name for a specific user.
     */
    fun findByUserIdAndName(userId: UUID, name: String): TagEntity?

    /**
     * Check if a tag name exists for a user.
     */
    fun existsByUserIdAndName(userId: UUID, name: String): Boolean

    /**
     * Delete all tags for a specific user.
     */
    fun deleteByUserId(userId: UUID)
}
