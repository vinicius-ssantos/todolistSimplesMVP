package com.viniss.todo.service.port

import com.viniss.todo.service.model.TodoListView

interface TodoListReadRepository {
    fun findAllWithTasksOrdered(): List<TodoListView>
}
