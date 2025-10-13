package com.viniss.todo.repo

import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView

internal fun TodoListEntity.toView(): TodoListView = TodoListView(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    tasks = tasks
        .sortedBy(TaskEntity::position)
        .map(TaskEntity::toView)
)

internal fun TaskEntity.toView(): TaskView = TaskView(
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
