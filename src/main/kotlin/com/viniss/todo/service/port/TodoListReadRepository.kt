package com.viniss.todo.service.port

import com.viniss.todo.service.model.TodoListView
import java.util.UUID

interface TodoListReadRepository {
    fun findAllWithTasksOrdered(): List<TodoListView>
    fun findByIdWithTasks(listId: UUID): TodoListView?
}
