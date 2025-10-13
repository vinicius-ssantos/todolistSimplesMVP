package com.viniss.todo.api.mapper

import com.viniss.todo.api.dto.CreateTaskRequest
import com.viniss.todo.api.dto.CreateTodoListRequest
import com.viniss.todo.service.model.CreateTaskCommand
import com.viniss.todo.service.model.CreateTodoListCommand

object RequestMapper {

    fun CreateTodoListRequest.toCommand() = CreateTodoListCommand(
        name = name
    )

    fun CreateTaskRequest.toCommand() = CreateTaskCommand(
        title = title,
        notes = notes,
        priority = priority,
        status = status,
        dueDate = dueDate,
        position = position
    )
}
