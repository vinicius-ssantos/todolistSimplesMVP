package com.viniss.todo.service.port

import java.util.UUID

interface DeleteTaskUseCase {
    fun delete(listId: UUID, taskId: UUID)
}
