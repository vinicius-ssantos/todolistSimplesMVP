package com.viniss.todo.resource

import com.viniss.todo.service.exception.TodoListNotFoundException
import org.springframework.http.HttpStatus
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

    @ExceptionHandler(TodoListNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: TodoListNotFoundException) = ApiErrorResponse(
        message = ex.message ?: "Resource not found"
    )
}
