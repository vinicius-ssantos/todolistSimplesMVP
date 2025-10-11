package com.viniss.todo.api.mapper

import com.viniss.todo.api.dto.TaskResponse
import com.viniss.todo.api.dto.TodoListResponse
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView

object ResponseMapper {

    fun TaskView.toResponse() = TaskResponse(
        id = id,
        title = title,
        notes = notes,
        priority = priority,
        status = status,
        dueDate = dueDate,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun TodoListView.toResponse() = TodoListResponse(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tasks = tasks.map { it.toResponse() }
    )
}