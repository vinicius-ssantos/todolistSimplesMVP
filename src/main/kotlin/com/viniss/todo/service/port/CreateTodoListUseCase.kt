package com.viniss.todo.service.port

import com.viniss.todo.service.model.CreateTodoListCommand
import com.viniss.todo.service.model.TodoListView

interface CreateTodoListUseCase {
    fun create(command: CreateTodoListCommand): TodoListView
}
