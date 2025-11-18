package com.viniss.todo.api.dto

import com.viniss.todo.validation.NoHtml
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateTodoListRequest(
    @field:NotBlank(message = "Todo list name must not be blank")
    @field:Size(min = 1, max = 100, message = "Todo list name must be between 1 and 100 characters")
    @field:NoHtml(message = "Todo list name cannot contain HTML or script tags")
    val name: String
)
