package com.viniss.todo.resource

import java.time.Instant

data class ApiErrorResponse(
    val message: String,
    val timestamp: Instant = Instant.now()
)
