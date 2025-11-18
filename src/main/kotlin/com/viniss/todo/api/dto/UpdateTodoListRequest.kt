package com.viniss.todo.api.dto

import jakarta.validation.constraints.Size

data class UpdateTodoListRequest(
    @field:Size(min = 1, max = 100, message = "Todo list name must not be blank")
    val name: String? = null
)