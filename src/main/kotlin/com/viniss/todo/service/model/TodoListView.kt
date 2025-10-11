package com.viniss.todo.service.model

import java.time.Instant
import java.util.*


data class TodoListView(
    val id: UUID,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val tasks: List<TaskView>
)
