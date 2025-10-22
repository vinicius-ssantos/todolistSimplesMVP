package com.viniss.todo.service.port

import com.viniss.todo.api.dto.TaskFilters
import com.viniss.todo.service.model.TaskView
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface ListTasksUseCase {
  fun listByFilters(listId: UUID, filters: TaskFilters, pageable: Pageable): Page<TaskView>
}
