package com.viniss.todo.repo

import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.repo.mapper.EntityMappers
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.TodoListReadRepository
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class JpaTodoListReadRepository(
    private val todoListRepository: TodoListRepository,
    private val taskRepository: TaskRepository,
    private val entityMappers: EntityMappers
) : TodoListReadRepository {

    override fun findAllWithTasksOrdered(): List<TodoListView> =
        todoListRepository.findAllWithTasksOrdered()
            .distinctBy(TodoListEntity::id)
            .sortedBy(TodoListEntity::createdAt)
            .map(entityMappers::mapToView)

    override fun findByIdWithTasks(listId: UUID): TodoListView? =
        todoListRepository.findByIdWithTasks(listId)?.let(entityMappers::mapToView)

    override fun findTaskById(listId: UUID, taskId: UUID): TaskView? {
        // First verify the list exists
        val list = todoListRepository.findByIdWithTasks(listId) ?: return null
        
        // Find the task and verify it belongs to the list
        return taskRepository.findById(taskId)
            .orElse(null)
            ?.takeIf { it.list.id == listId }
            ?.let(entityMappers::mapToView)
    }
}
