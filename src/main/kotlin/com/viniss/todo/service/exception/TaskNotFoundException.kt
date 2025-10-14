package com.viniss.todo.service.exception

import java.util.UUID

class TaskNotFoundException(taskId: UUID) : RuntimeException("Task with id $taskId not found")
