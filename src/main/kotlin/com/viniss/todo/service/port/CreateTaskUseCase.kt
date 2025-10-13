package com.viniss.todo.service.port

import com.viniss.todo.service.model.CreateTaskCommand
import com.viniss.todo.service.model.TaskView
import java.util.UUID

interface CreateTaskUseCase {
    fun create(listId: UUID, command: CreateTaskCommand): TaskView
}
