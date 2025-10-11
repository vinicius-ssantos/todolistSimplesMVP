package com.viniss.todo.repo
import com.viniss.todo.domain.TaskEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TaskRepository : JpaRepository<TaskEntity, UUID> {
    fun findByListIdOrderByPositionAsc(listId: UUID): List<TaskEntity>
}