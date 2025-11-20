package com.viniss.todo.service.task

import com.viniss.todo.service.exception.TodoListNotFoundException
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.UpdateTaskCommand
import com.viniss.todo.service.port.TodoListReadRepository
import com.viniss.todo.service.port.UpdateTaskUseCase
import com.viniss.todo.service.port.write.TaskUpdater
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service responsible for updating Tasks.
 *
 * Follows SOLID principles:
 * - SRP: Only handles Task updates
 * - ISP: Depends only on TaskUpdater (1 method) instead of TodoListWriteRepository (6 methods)
 *
 * Benefits:
 * - Clearer dependencies (only updateTask() is needed)
 * - Easier to test (mock only TaskUpdater)
 * - Better encapsulation (no access to unneeded list/create/delete methods)
 */
@Service
@Primary
class UpdateTaskService(
    private val taskUpdater: TaskUpdater,
    private val todoListReadRepository: TodoListReadRepository
) : UpdateTaskUseCase {

    @Transactional
    override fun update(listId: UUID, taskId: UUID, command: UpdateTaskCommand): TaskView {
        todoListReadRepository.findByIdWithTasks(listId)
            ?: throw TodoListNotFoundException(listId)

        val updates = mutableMapOf<String, Any?>()

        command.title?.let {
            val trimmedTitle = it.trim()
            require(trimmedTitle.isNotEmpty()) { "Task title must not be blank" }
            updates["title"] = trimmedTitle
        }

        command.notes?.let { updates["notes"] = it.trim().takeIf { it.isNotEmpty() } }
        command.priority?.let { updates["priority"] = it }
        command.status?.let { updates["status"] = it }
        command.dueDate?.let { updates["dueDate"] = it }
        command.position?.let {
            require(it >= 0) { "Task position must be zero or positive" }
            updates["position"] = it
        }

        return taskUpdater.updateTask(listId, taskId, updates)
    }
}
