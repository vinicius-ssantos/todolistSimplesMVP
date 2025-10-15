package com.viniss.todo.service

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import com.viniss.todo.service.exception.TodoListNotFoundException
import com.viniss.todo.service.exception.TaskNotFoundException
import com.viniss.todo.service.model.CreateTaskCommand
import com.viniss.todo.service.model.CreateTodoListCommand
import com.viniss.todo.service.model.UpdateTaskCommand
import com.viniss.todo.service.model.UpdateTodoListCommand
import com.viniss.todo.service.model.TaskCreationData
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.CreateTaskUseCase
import com.viniss.todo.service.port.CreateTodoListUseCase
import com.viniss.todo.service.port.DeleteTaskUseCase
import com.viniss.todo.service.port.DeleteTodoListUseCase
import com.viniss.todo.service.port.UpdateTaskUseCase
import com.viniss.todo.service.port.UpdateTodoListUseCase
import com.viniss.todo.service.port.TodoListReadRepository
import com.viniss.todo.service.port.TodoListWriteRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TodoListCommandService(
    private val todoListWriteRepository: TodoListWriteRepository,
    private val todoListReadRepository: TodoListReadRepository
) : CreateTodoListUseCase, CreateTaskUseCase, UpdateTodoListUseCase, UpdateTaskUseCase, DeleteTodoListUseCase, DeleteTaskUseCase {

    @Transactional
    override fun create(command: CreateTodoListCommand): TodoListView {
        val trimmedName = command.name.trim()
        require(trimmedName.isNotEmpty()) { "Todo list name must not be blank" }
        return todoListWriteRepository.createList(trimmedName)
    }

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

        return todoListWriteRepository.addTask(listId, taskCreation)
    }

    @Transactional
    override fun update(listId: UUID, command: UpdateTodoListCommand): TodoListView {
        val trimmedName = command.name?.trim()
        require(trimmedName?.isNotEmpty() == true) { "Todo list name must not be blank" }
        return todoListWriteRepository.updateList(listId, trimmedName!!)
    }

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

        return todoListWriteRepository.updateTask(listId, taskId, updates)
    }

    @Transactional
    override fun delete(listId: UUID) {
        todoListWriteRepository.deleteList(listId)
    }

    @Transactional
    override fun delete(listId: UUID, taskId: UUID) {
        todoListWriteRepository.deleteTask(listId, taskId)
    }
}
