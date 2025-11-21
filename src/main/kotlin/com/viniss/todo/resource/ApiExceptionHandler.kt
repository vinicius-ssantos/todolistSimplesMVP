package com.viniss.todo.resource

import com.viniss.todo.service.exception.TodoListNotFoundException
import com.viniss.todo.service.exception.TaskNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException) = ApiErrorResponse(
        message = ex.message ?: "Invalid request"
    )

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalState(ex: IllegalStateException) = ApiErrorResponse(
        message = ex.message ?: "Invalid state"
    )

    @ExceptionHandler(TodoListNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleTodoListNotFound(ex: TodoListNotFoundException) = ApiErrorResponse(
        message = ex.message ?: "Todo list not found"
    )

    @ExceptionHandler(TaskNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleTaskNotFound(ex: TaskNotFoundException) = ApiErrorResponse(
        message = ex.message ?: "Task not found"
    )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException) = ApiErrorResponse(
        message = "Invalid JSON format or invalid enum value. ${ex.message}"
    )
}
