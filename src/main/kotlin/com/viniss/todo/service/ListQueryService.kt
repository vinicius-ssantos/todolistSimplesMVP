package com.viniss.todo.service

import com.viniss.todo.service.exception.TodoListNotFoundException
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.ListQueryUseCase
import com.viniss.todo.service.port.TodoListReadRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListQueryService(private val todoListReadRepository: TodoListReadRepository) : ListQueryUseCase {

    @Transactional(readOnly = true)
    override fun findAllWithTasks(): List<TodoListView> =
        todoListReadRepository.findAllWithTasksOrdered()

    @Transactional(readOnly = true)
    override fun findAllWithTasks(pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<TodoListView> =
        todoListReadRepository.findAllWithTasksOrdered(pageable)

    @Transactional(readOnly = true)
    override fun findById(listId: UUID): TodoListView =
        todoListReadRepository.findByIdWithTasks(listId)
            ?: throw TodoListNotFoundException(listId)
}
