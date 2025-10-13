package com.viniss.todo.repo

import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.service.exception.TodoListNotFoundException
import com.viniss.todo.service.model.TaskCreationData
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.TodoListWriteRepository
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class JpaTodoListWriteRepository(
    private val todoListRepository: TodoListRepository,
    private val taskRepository: TaskRepository
) : TodoListWriteRepository {

    override fun createList(name: String): TodoListView {
        val entity = TodoListEntity(name = name)
        val saved = todoListRepository.save(entity)
        return saved.toView()
    }

    override fun addTask(listId: UUID, task: TaskCreationData): TaskView {
        val list = todoListRepository.findById(listId)
            .orElseThrow { TodoListNotFoundException(listId) }

        val taskEntity = TaskEntity(
            list = list,
            title = task.title,
            notes = task.notes,
            priority = task.priority,
            status = task.status,
            dueDate = task.dueDate,
            position = task.position
        )

        val saved = taskRepository.save(taskEntity)
        return saved.toView()
    }
}
