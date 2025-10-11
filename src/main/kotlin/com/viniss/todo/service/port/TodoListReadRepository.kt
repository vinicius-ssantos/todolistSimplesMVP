package com.viniss.todo.service.port

import com.viniss.todo.domain.TodoListEntity

interface TodoListReadRepository {
    fun findAllWithTasksOrdered(): List<TodoListEntity>
}
