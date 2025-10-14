package com.viniss.todo.service.port

import com.viniss.todo.service.model.TodoListView
import java.util.UUID

interface ListQueryUseCase {
    fun findAllWithTasks(): List<TodoListView>
    fun findById(listId: UUID): TodoListView
}
