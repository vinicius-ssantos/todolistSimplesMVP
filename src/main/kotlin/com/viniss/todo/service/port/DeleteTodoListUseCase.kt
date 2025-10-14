package com.viniss.todo.service.port

import java.util.UUID

interface DeleteTodoListUseCase {
    fun delete(listId: UUID)
}
