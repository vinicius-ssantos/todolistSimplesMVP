package com.viniss.todo.repo

import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TodoListRepository : JpaRepository<TodoListEntity, UUID>
interface TaskRepository : JpaRepository<TaskEntity, UUID> {
    fun findByListIdOrderByPositionAsc(listId: UUID): List<TaskEntity>
}
