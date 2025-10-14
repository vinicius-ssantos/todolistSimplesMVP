package com.viniss.todo.service.port

import com.viniss.todo.service.model.UpdateTaskCommand
import com.viniss.todo.service.model.TaskView
import java.util.UUID

interface UpdateTaskUseCase {
    fun update(listId: UUID, taskId: UUID, command: UpdateTaskCommand): TaskView
}
