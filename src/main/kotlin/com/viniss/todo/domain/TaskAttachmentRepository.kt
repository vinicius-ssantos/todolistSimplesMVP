package com.viniss.todo.domain

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TaskAttachmentRepository : JpaRepository<TaskAttachmentEntity, UUID> {

    /**
     * Find all attachments for a specific task.
     */
    fun findByTaskId(taskId: UUID): List<TaskAttachmentEntity>

    /**
     * Find all attachments for a specific user.
     */
    fun findByUserId(userId: UUID): List<TaskAttachmentEntity>

    /**
     * Delete all attachments for a specific task.
     */
    fun deleteByTaskId(taskId: UUID)

    /**
     * Count attachments for a specific task.
     */
    fun countByTaskId(taskId: UUID): Long
}
