package com.viniss.todo.service.task

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import com.viniss.todo.service.exception.TodoListNotFoundException
import com.viniss.todo.service.model.CreateTaskCommand
import com.viniss.todo.service.model.TaskCreationData
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.port.CreateTaskUseCase
import com.viniss.todo.service.port.TodoListReadRepository
import com.viniss.todo.service.port.write.TaskCreator
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service responsible for creating Tasks.
 *
 * Follows SOLID principles:
 * - SRP: Only handles Task creation
 * - ISP: Depends only on TaskCreator (1 method) instead of TodoListWriteRepository (6 methods)
 *
 * Benefits:
 * - Clearer dependencies (only addTask() is needed)
 * - Easier to test (mock only TaskCreator)
 * - Better encapsulation (no access to unneeded list/update/delete methods)
 */
@Service
@Primary
class CreateTaskService(
    private val taskCreator: TaskCreator,
    private val todoListReadRepository: TodoListReadRepository
) : CreateTaskUseCase {

    @Transactional
    override fun create(listId: UUID, command: CreateTaskCommand): TaskView {
        val list = todoListReadRepository.findByIdWithTasks(listId)
            ?: throw TodoListNotFoundException(listId)

        val trimmedTitle = command.title.trim()
        require(trimmedTitle.isNotEmpty()) { "Task title must not be blank" }

        val finalPosition = command.position ?: list.tasks.maxOfOrNull(TaskView::position)?.plus(1) ?: 0
        require(finalPosition >= 0) { "Task position must be zero or positive" }
        require(list.tasks.none { it.position == finalPosition }) {
            "Task position $finalPosition is already in use"
        }

        val taskCreation = TaskCreationData(
            title = trimmedTitle,
            notes = command.notes,
            priority = command.priority ?: Priority.MEDIUM,
            status = command.status ?: Status.OPEN,
            dueDate = command.dueDate,
            position = finalPosition
        )

        return taskCreator.addTask(listId, taskCreation)
    }
}
