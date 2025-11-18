package com.viniss.todo.service.port

import com.viniss.todo.service.model.TodoListView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ListQueryUseCase {
    fun findAllWithTasks(): List<TodoListView>
    fun findAllWithTasks(pageable: Pageable): Page<TodoListView>
    fun findById(listId: UUID): TodoListView
}
