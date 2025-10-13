package com.viniss.todo.repo

import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.TodoListReadRepository
import org.springframework.stereotype.Component

@Component
class JpaTodoListReadRepository(private val todoListRepository: TodoListRepository) : TodoListReadRepository {
    override fun findAllWithTasksOrdered(): List<TodoListView> =
        todoListRepository.findAllWithTasksOrdered()
            .distinctBy(TodoListEntity::id)
            .sortedBy(TodoListEntity::createdAt)
            .map { it.toView() }

    private fun TodoListEntity.toView(): TodoListView = TodoListView(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tasks = tasks
            .sortedBy(TaskEntity::position)
            .map { it.toView() }
    )

    private fun TaskEntity.toView(): TaskView = TaskView(
        id = id,
        title = title,
        notes = notes,
        priority = priority,
        status = status,
        dueDate = dueDate,
        position = position,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
