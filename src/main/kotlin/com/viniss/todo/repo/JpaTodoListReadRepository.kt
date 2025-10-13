package com.viniss.todo.repo

import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.TodoListReadRepository
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class JpaTodoListReadRepository(
    private val todoListRepository: TodoListRepository
) : TodoListReadRepository {

    override fun findAllWithTasksOrdered(): List<TodoListView> =
        todoListRepository.findAllWithTasksOrdered()
            .distinctBy(TodoListEntity::id)
            .sortedBy(TodoListEntity::createdAt)
            .map(TodoListEntity::toView)

    override fun findByIdWithTasks(listId: UUID): TodoListView? =
        todoListRepository.findByIdWithTasks(listId)?.toView()
}
