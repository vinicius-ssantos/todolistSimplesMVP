package com.viniss.todo.repo.mapper

import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import org.springframework.stereotype.Component

@Component
class EntityMappers {

    fun mapToView(todoListEntity: TodoListEntity): TodoListView = TodoListView(
        id = todoListEntity.id,
        name = todoListEntity.name,
        createdAt = todoListEntity.createdAt,
        updatedAt = todoListEntity.updatedAt,
        tasks = todoListEntity.tasks
            .sortedBy(TaskEntity::position)
            .map(this::mapToView)
    )

    fun mapToView(taskEntity: TaskEntity): TaskView = TaskView(
        id = taskEntity.id,
        title = taskEntity.title,
        notes = taskEntity.notes,
        priority = taskEntity.priority,
        status = taskEntity.status,
        dueDate = taskEntity.dueDate,
        position = taskEntity.position,
        createdAt = taskEntity.createdAt,
        updatedAt = taskEntity.updatedAt
    )
}
