package com.viniss.todo.service

import com.viniss.todo.service.exception.TaskNotFoundException
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.port.GetTaskUseCase
import com.viniss.todo.service.port.TodoListReadRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TaskQueryService(
    private val todoListReadRepository: TodoListReadRepository
) : GetTaskUseCase {

    @Transactional(readOnly = true)
    override fun findById(listId: UUID, taskId: UUID): TaskView =
        todoListReadRepository.findTaskById(listId, taskId)
            ?: throw TaskNotFoundException(taskId)
}
