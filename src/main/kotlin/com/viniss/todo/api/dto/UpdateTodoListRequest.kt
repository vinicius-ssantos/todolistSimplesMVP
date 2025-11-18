package com.viniss.todo.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateTodoListRequest(
    @field:NotBlank(message = "Todo list name must not be blank")
    @field:Size(min = 1, max = 100, message = "Todo list name must be between 1 and 100 characters")
    val name: String? = null
)