package com.viniss.todo.service.list

import com.viniss.todo.service.port.DeleteTodoListUseCase
import com.viniss.todo.service.port.TodoListWriteRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Serviço responsável pela remoção de listas de tarefas.
 * Implementa o princípio de responsabilidade única (SRP).
 */
@Service
@Primary
class DeleteTodoListService(
    private val todoListWriteRepository: TodoListWriteRepository
) : DeleteTodoListUseCase {

    @Transactional
    override fun delete(listId: UUID) {
        todoListWriteRepository.deleteList(listId)
    }
}
