package com.viniss.todo.repo

import com.viniss.todo.auth.CurrentUser
import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.repo.mapper.EntityMappers
import com.viniss.todo.service.exception.TodoListNotFoundException
import com.viniss.todo.service.exception.TaskNotFoundException
import com.viniss.todo.service.model.TaskCreationData
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.TodoListWriteRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class JpaTodoListWriteRepository(
    private val todoListRepository: TodoListRepository,
    private val taskRepository: TaskRepository,
    private val entityMappers: EntityMappers,
    private val currentUser: CurrentUser
) : TodoListWriteRepository {

    override fun createList(name: String): TodoListView {
        val uid = currentUser.id()
        val entity = TodoListEntity(name = name).apply { userId = uid }
        return entityMappers.mapToView(todoListRepository.save(entity))
    }

    override fun addTask(listId: UUID, task: TaskCreationData): TaskView {
        val uid = currentUser.id()
        val list = todoListRepository.findByIdWithTasksAndUser(listId, uid)
            ?: throw TodoListNotFoundException(listId)

        val taskEntity = TaskEntity(
            list = list,
            title = task.title,
            notes = task.notes,
            priority = task.priority,
            status = task.status,
            dueDate = task.dueDate,
            position = task.position
        ).apply {
            this.list = list
            this.userId = uid
        }

        return entityMappers.mapToView(taskRepository.save(taskEntity))
    }

    @Transactional
    override fun updateList(listId: UUID, name: String): TodoListView {
        val uid = currentUser.id()
        val list = todoListRepository.findByIdWithTasksAndUser(listId, uid)
            ?: throw TodoListNotFoundException(listId)
        list.name = name
        return entityMappers.mapToView(todoListRepository.save(list))
    }

    @Transactional
    override fun updateTask(listId: UUID, taskId: UUID, updates: Map<String, Any?>): TaskView {
        val uid = currentUser.id()
        val list = todoListRepository.findByIdWithTasksAndUser(listId, uid)
            ?: throw TodoListNotFoundException(listId)

        val task = taskRepository.findByIdAndUserId(taskId, uid)
            ?: throw TaskNotFoundException(taskId)

        if (task.list.id != list.id) throw TaskNotFoundException(taskId)

        
        // Apply updates
        updates.forEach { (field, value) ->
            when (field) {
                "title" -> if (value != null) task.title = value as String
                "notes" -> task.notes = value as String?
                "priority" -> if (value != null) task.priority = value as com.viniss.todo.domain.Priority
                "status" -> if (value != null) task.status = value as com.viniss.todo.domain.Status
                "dueDate" -> task.dueDate = value as java.time.LocalDate?
                "position" -> if (value != null) {
                    val newPosition = value as Int
                    if (newPosition != task.position) {
                        reorganizeTaskPositions(list, task, newPosition)
                        task.position = newPosition
                    }
                }
            }
        }

        return entityMappers.mapToView(taskRepository.save(task))
    }
    
    private fun reorganizeTaskPositions(list: TodoListEntity, movedTask: TaskEntity, newPosition: Int) {
        val allTasks = taskRepository.findByListIdOrderByPositionAsc(list.id)
        val oldPosition = movedTask.position
        
        allTasks.forEach { task ->
            when {
                task.id == movedTask.id -> {
                    // Skip the moved task itself
                }
                oldPosition < newPosition -> {
                    // Moving task down: shift tasks between old and new position up
                    if (task.position > oldPosition && task.position <= newPosition) {
                        task.position--
                    }
                }
                oldPosition > newPosition -> {
                    // Moving task up: shift tasks between new and old position down
                    if (task.position >= newPosition && task.position < oldPosition) {
                        task.position++
                    }
                }
            }
        }
        
        // Save all affected tasks
        taskRepository.saveAll(allTasks.filter { it.id != movedTask.id })
    }

    @Transactional
    override fun deleteList(listId: UUID) {
        val uid = currentUser.id()
        val list = todoListRepository.findByIdWithTasksAndUser(listId, uid)
            ?: throw TodoListNotFoundException(listId)
        todoListRepository.delete(list)
    }

    @Transactional
    override fun deleteTask(listId: UUID, taskId: UUID) {
        val uid = currentUser.id()
        val listExists = todoListRepository.existsByIdAndUserId(listId, uid)
        if (!listExists) throw TodoListNotFoundException(listId)

        val deletedRows = taskRepository.deleteByIdAndUserIdAndListId(taskId, uid, listId)
        if (deletedRows == 0) throw TaskNotFoundException(taskId)
    }
}
