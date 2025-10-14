package com.viniss.todo.service.port

import com.viniss.todo.service.model.TaskView
import java.util.UUID

interface GetTaskUseCase {
    fun findById(listId: UUID, taskId: UUID): TaskView
}
