package com.viniss.todo.api.dto

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UpdateTaskRequest(
    @field:NotBlank(message = "Task title must not be blank")
    @field:Size(min = 1, max = 200, message = "Task title must be between 1 and 200 characters")
    val title: String? = null,

    @field:Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    val notes: String? = null,

    val priority: Priority? = null,
    val status: Status? = null,
    val dueDate: LocalDate? = null,

    @field:Min(value = 0, message = "Task position must be zero or positive")
    val position: Int? = null
)
