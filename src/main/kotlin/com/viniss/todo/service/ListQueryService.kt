package com.viniss.todo.service

import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.ListQueryUseCase
import com.viniss.todo.service.port.TodoListReadRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListQueryService(
    private val todoListReadRepository: TodoListReadRepository
) : ListQueryUseCase {
    @Transactional(readOnly = true)
    override fun findAllWithTasks(): List<TodoListView> =
        todoListReadRepository
            .findAllWithTasksOrdered()
            .distinctBy { it.id }
            .sortedBy { it.createdAt }
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
