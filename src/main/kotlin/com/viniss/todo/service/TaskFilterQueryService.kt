package com.viniss.todo.service

import com.viniss.todo.api.dto.TaskFilters
import com.viniss.todo.repo.TaskRepository
import com.viniss.todo.repo.spec.TaskSpecifications
import com.viniss.todo.repo.mapper.EntityMappers
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.port.ListTasksUseCase
import com.viniss.todo.auth.CurrentUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TaskFilterQueryService(
  private val taskRepository: TaskRepository,
  private val entityMappers: EntityMappers,
  private val currentUser: CurrentUser
) : ListTasksUseCase {

  @Transactional(readOnly = true)
  override fun listByFilters(listId: UUID, filters: TaskFilters, pageable: Pageable): Page<TaskView> {
    val spec = TaskSpecifications.allOf(
      listId = listId,
      userId = currentUser.id(),
      due = filters.due,
      statuses = filters.statuses,
      q = filters.search,
      priority = filters.priority
    )
    val sort = if (pageable.sort.isSorted) pageable.sort else Sort.by("position").ascending()
    val pageReq = PageRequest.of(pageable.pageNumber, pageable.pageSize, sort)
    return taskRepository.findAll(spec, pageReq).map(entityMappers::mapToView)
  }
}
