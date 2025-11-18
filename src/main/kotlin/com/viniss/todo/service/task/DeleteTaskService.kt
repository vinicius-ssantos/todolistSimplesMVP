package com.viniss.todo.service.task

import com.viniss.todo.service.port.DeleteTaskUseCase
import com.viniss.todo.service.port.TodoListWriteRepository
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Serviço responsável pela remoção de tarefas.
 * Implementa o princípio de responsabilidade única (SRP).
 */
@Service
@Primary
class DeleteTaskService(
    private val todoListWriteRepository: TodoListWriteRepository
) : DeleteTaskUseCase {

    @Transactional
    override fun delete(listId: UUID, taskId: UUID) {
        todoListWriteRepository.deleteTask(listId, taskId)
    }
}
