package com.viniss.todo.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateTodoListRequest(
    @field:NotBlank(message = "Nome da lista é obrigatório")
    @field:Size(min = 1, max = 100, message = "Nome da lista deve ter entre 1 e 100 caracteres")
    val name: String
)
