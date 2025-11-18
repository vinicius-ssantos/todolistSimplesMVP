package com.viniss.todo.api.dto

import com.viniss.todo.validation.NoHtml
import jakarta.validation.constraints.Size

data class UpdateTodoListRequest(
    @field:Size(min = 1, max = 100, message = "Todo list name must not be blank")
    @field:NoHtml(message = "Todo list name cannot contain HTML or script tags")
    val name: String? = null
)