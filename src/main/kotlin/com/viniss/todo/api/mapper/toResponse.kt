package com.viniss.todo.api.mapper

import com.viniss.todo.api.dto.TaskResponse
import com.viniss.todo.api.dto.TodoListResponse
import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity

fun TaskEntity.toResponse() = TaskResponse(
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

fun TodoListEntity.toResponse(tasks: List<TaskResponse>) = TodoListResponse(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tasks = tasks
)