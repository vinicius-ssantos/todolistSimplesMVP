package com.viniss.todo.service.model

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import java.time.LocalDate

data class CreateTaskCommand(
    val title: String,
    val notes: String? = null,
    val priority: Priority? = null,
    val status: Status? = null,
    val dueDate: LocalDate? = null,
    val position: Int? = null
)
