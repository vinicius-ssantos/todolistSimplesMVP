package com.viniss.todo.service.port

import com.viniss.todo.service.model.UpdateTodoListCommand
import com.viniss.todo.service.model.TodoListView
import java.util.UUID

interface UpdateTodoListUseCase {
    fun update(listId: UUID, command: UpdateTodoListCommand): TodoListView
}
