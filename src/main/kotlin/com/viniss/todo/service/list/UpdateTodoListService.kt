package com.viniss.todo.service.list

import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.model.UpdateTodoListCommand
import com.viniss.todo.service.port.TodoListWriteRepository
import com.viniss.todo.service.port.UpdateTodoListUseCase
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Serviço responsável pela atualização de listas de tarefas.
 * Implementa o princípio de responsabilidade única (SRP).
 */
@Service
@Primary
class UpdateTodoListService(
    private val todoListWriteRepository: TodoListWriteRepository
) : UpdateTodoListUseCase {

    @Transactional
    override fun update(listId: UUID, command: UpdateTodoListCommand): TodoListView {
        val trimmedName = command.name?.trim()
        require(trimmedName?.isNotEmpty() == true) { "Todo list name must not be blank" }

        return todoListWriteRepository.updateList(listId, trimmedName!!)
    }
}
