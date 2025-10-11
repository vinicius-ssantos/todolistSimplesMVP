package com.viniss.todo.service.port

import com.viniss.todo.service.model.TodoListView

interface ListQueryUseCase {
    fun findAllWithTasks(): List<TodoListView>
}
