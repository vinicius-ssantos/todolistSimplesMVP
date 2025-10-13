package com.viniss.todo.api.dto

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import java.time.LocalDate

data class CreateTaskRequest(
    val title: String,
    val notes: String? = null,
    val priority: Priority? = null,
    val status: Status? = null,
    val dueDate: LocalDate? = null,
    val position: Int? = null
)
