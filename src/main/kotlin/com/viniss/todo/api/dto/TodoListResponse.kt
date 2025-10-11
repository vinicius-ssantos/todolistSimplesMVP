package com.viniss.todo.api.dto
import java.time.Instant
import java.util.*

data class TodoListResponse(
    val id: UUID,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val tasks: List<TaskResponse>
)