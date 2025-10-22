package com.viniss.todo.api.dto

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status

data class TaskFilters(
    val due: DueFilter = DueFilter.ALL,
    val statuses: Set<Status>? = null,   // null = sem filtro
    val search: String? = null,
    val priority: Priority? = null
)
