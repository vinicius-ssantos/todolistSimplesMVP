package com.viniss.todo.api.mapper

import com.viniss.todo.api.dto.CreateTaskRequest
import com.viniss.todo.api.dto.CreateTodoListRequest
import com.viniss.todo.api.dto.UpdateTaskRequest
import com.viniss.todo.api.dto.UpdateTodoListRequest
import com.viniss.todo.service.model.CreateTaskCommand
import com.viniss.todo.service.model.CreateTodoListCommand
import com.viniss.todo.service.model.UpdateTaskCommand
import com.viniss.todo.service.model.UpdateTodoListCommand

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

    fun UpdateTodoListRequest.toCommand() = UpdateTodoListCommand(
        name = name
    )

    fun UpdateTaskRequest.toCommand() = UpdateTaskCommand(
        title = title,
        notes = notes,
        priority = priority,
        status = status,
        dueDate = dueDate,
        position = position
    )
}
