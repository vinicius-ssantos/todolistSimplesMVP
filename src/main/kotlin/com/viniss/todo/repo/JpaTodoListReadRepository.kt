package com.viniss.todo.repo

import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.service.port.TodoListReadRepository
import org.springframework.stereotype.Component

@Component
class JpaTodoListReadRepository(
    private val todoListRepository: TodoListRepository
) : TodoListReadRepository {
    override fun findAllWithTasksOrdered(): List<TodoListEntity> =
        todoListRepository.findAllWithTasksOrdered()
}
