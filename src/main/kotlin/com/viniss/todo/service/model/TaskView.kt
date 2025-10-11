package com.viniss.todo.service.model

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class TaskView(
    val id: UUID,
    val title: String,
    val notes: String?,
    val priority: Priority,
    val status: Status,
    val dueDate: LocalDate?,
    val position: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)