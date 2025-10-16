package com.viniss.todo.repo

import com.viniss.todo.domain.TaskEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface TaskRepository : JpaRepository<TaskEntity, UUID> {
    fun findByListIdOrderByPositionAsc(listId: UUID): List<TaskEntity>
    fun findByIdAndUserId(id: UUID, userId: UUID): TaskEntity?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            delete from TaskEntity t
            where t.id = :taskId and t.list.id = :listId and t.userId = :userId
        """
    )
    fun deleteOwned(taskId: UUID, listId: UUID, userId: UUID): Int
}
