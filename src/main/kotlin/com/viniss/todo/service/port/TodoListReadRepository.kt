package com.viniss.todo.service.port

import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface TodoListReadRepository {
    fun findAllWithTasksOrdered(): List<TodoListView>
    fun findAllWithTasksOrdered(pageable: Pageable): Page<TodoListView>
    fun findByIdWithTasks(listId: UUID): TodoListView?
    fun findTaskById(listId: UUID, taskId: UUID): TaskView?
}
