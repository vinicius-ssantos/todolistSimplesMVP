package com.viniss.todo.api.dto

import jakarta.validation.constraints.Size

data class UpdateTodoListRequest(
    @field:Size(min = 1, max = 100, message = "Nome da lista deve ter entre 1 e 100 caracteres")
    val name: String? = null
)