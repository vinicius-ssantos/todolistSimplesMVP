package com.viniss.todo.repo

import com.viniss.todo.auth.CurrentUser
import com.viniss.todo.repo.mapper.EntityMappers
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.TodoListReadRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class JpaTodoListReadRepository(
    private val todoListRepository: TodoListRepository,
    private val taskRepository: TaskRepository,
    private val entityMappers: EntityMappers,
    private val currentUser: CurrentUser
) : TodoListReadRepository {

    override fun findAllWithTasksOrdered(): List<TodoListView> =
        todoListRepository.findAllWithTasksOrderedByUser(currentUser.id())
            .map(entityMappers::mapToView)

    override fun findByIdWithTasks(listId: UUID): TodoListView? =
        todoListRepository.findByIdWithTasksAndUser(listId, currentUser.id())?.let(entityMappers::mapToView)

    override fun findTaskById(listId: UUID, taskId: UUID): TaskView? {
        val uid = currentUser.id()
        // 1) lista precisa ser do dono
        val list = todoListRepository.findByIdWithTasksAndUser(listId, uid) ?: return null
        // 2) task precisa ser do dono e pertencer à lista
        return taskRepository.findByIdAndUserId(taskId, uid)
            ?.takeIf { it.list.id == list.id }
            ?.let(entityMappers::mapToView)
    }
}
