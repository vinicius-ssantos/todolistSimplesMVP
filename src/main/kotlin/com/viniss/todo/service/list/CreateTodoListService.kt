package com.viniss.todo.service.list

import com.viniss.todo.service.model.CreateTodoListCommand
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.CreateTodoListUseCase
import com.viniss.todo.service.port.write.TodoListCreator
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service responsible for creating TodoLists.
 *
 * Follows SOLID principles:
 * - SRP: Only handles TodoList creation
 * - ISP: Depends only on TodoListCreator (1 method) instead of TodoListWriteRepository (6 methods)
 *
 * Benefits:
 * - Clearer dependencies (only createList() is needed)
 * - Easier to test (mock only TodoListCreator)
 * - Better encapsulation (no access to unneeded update/delete methods)
 */
@Service
@Primary
class CreateTodoListService(
    private val todoListCreator: TodoListCreator
) : CreateTodoListUseCase {

    @Transactional
    override fun create(command: CreateTodoListCommand): TodoListView {
        val trimmedName = command.name.trim()
        require(trimmedName.isNotEmpty()) { "Todo list name must not be blank" }

        return todoListCreator.createList(trimmedName)
    }
}
