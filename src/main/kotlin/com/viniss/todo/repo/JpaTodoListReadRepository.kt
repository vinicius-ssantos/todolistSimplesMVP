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

    override fun findAllWithTasksOrdered(pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<TodoListView> {
        val userId = currentUser.id()

        // First, get paginated IDs
        val idsPage = todoListRepository.findIdsByUser(userId, pageable)

        // If no results, return empty page
        if (idsPage.isEmpty) {
            return org.springframework.data.domain.PageImpl(emptyList(), pageable, 0)
        }

        // Then fetch full entities with tasks for those IDs
        val entities = todoListRepository.findByIdsWithTasks(idsPage.content, userId)

        // Map to views maintaining the order
        val viewsMap = entities.map(entityMappers::mapToView).associateBy { it.id }
        val orderedViews = idsPage.content.mapNotNull { viewsMap[it] }

        return org.springframework.data.domain.PageImpl(orderedViews, pageable, idsPage.totalElements)
    }

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
