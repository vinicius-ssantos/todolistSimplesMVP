package com.viniss.todo.service.model

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import java.time.LocalDate

data class TaskCreationData(
    val title: String,
    val notes: String?,
    val priority: Priority,
    val status: Status,
    val dueDate: LocalDate?,
    val position: Int
)
