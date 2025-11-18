package com.viniss.todo.api.dto

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateTaskRequest(
    @field:NotBlank(message = "Título da tarefa é obrigatório")
    @field:Size(min = 1, max = 200, message = "Título da tarefa deve ter entre 1 e 200 caracteres")
    val title: String,

    @field:Size(max = 1000, message = "Notas não podem exceder 1000 caracteres")
    val notes: String? = null,

    val priority: Priority? = null,
    val status: Status? = null,
    val dueDate: LocalDate? = null,

    @field:Min(value = 0, message = "Posição não pode ser negativa")
    val position: Int? = null
)
