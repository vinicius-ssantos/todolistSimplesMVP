package com.viniss.todo.repo

import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.repo.mapper.EntityMappers
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.TodoListReadRepository
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class JpaTodoListReadRepository(
    private val todoListRepository: TodoListRepository,
    private val entityMappers: EntityMappers
) : TodoListReadRepository {

    override fun findAllWithTasksOrdered(): List<TodoListView> =
        todoListRepository.findAllWithTasksOrdered()
            .distinctBy(TodoListEntity::id)
            .sortedBy(TodoListEntity::createdAt)
            .map(entityMappers::mapToView)

    override fun findByIdWithTasks(listId: UUID): TodoListView? =
        todoListRepository.findByIdWithTasks(listId)?.let(entityMappers::mapToView)
}
