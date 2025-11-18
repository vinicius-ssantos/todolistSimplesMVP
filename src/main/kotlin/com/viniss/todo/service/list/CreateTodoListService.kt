package com.viniss.todo.service.list

import com.viniss.todo.service.model.CreateTodoListCommand
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.CreateTodoListUseCase
import com.viniss.todo.service.port.TodoListWriteRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Serviço responsável pela criação de listas de tarefas.
 * Implementa o princípio de responsabilidade única (SRP).
 */
@Service
@Primary
class CreateTodoListService(
    private val todoListWriteRepository: TodoListWriteRepository
) : CreateTodoListUseCase {

    @Transactional
    override fun create(command: CreateTodoListCommand): TodoListView {
        val trimmedName = command.name.trim()
        require(trimmedName.isNotEmpty()) { "Todo list name must not be blank" }

        return todoListWriteRepository.createList(trimmedName)
    }
}
