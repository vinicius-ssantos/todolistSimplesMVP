package com.viniss.todo.service.list

import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.model.UpdateTodoListCommand
import com.viniss.todo.service.port.UpdateTodoListUseCase
import com.viniss.todo.service.port.write.TodoListUpdater
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service responsible for updating TodoLists.
 *
 * Follows SOLID principles:
 * - SRP: Only handles TodoList updates
 * - ISP: Depends only on TodoListUpdater (1 method) instead of TodoListWriteRepository (6 methods)
 *
 * Benefits:
 * - Clearer dependencies (only updateList() is needed)
 * - Easier to test (mock only TodoListUpdater)
 * - Better encapsulation (no access to unneeded create/delete methods)
 */
@Service
@Primary
class UpdateTodoListService(
    private val todoListUpdater: TodoListUpdater
) : UpdateTodoListUseCase {

    @Transactional
    override fun update(listId: UUID, command: UpdateTodoListCommand): TodoListView {
        val trimmedName = command.name?.trim()
        require(trimmedName?.isNotEmpty() == true) { "Todo list name must not be blank" }

        return todoListUpdater.updateList(listId, trimmedName!!)
    }
}
