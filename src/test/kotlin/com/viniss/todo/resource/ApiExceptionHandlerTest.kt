package com.viniss.todo.resource

import com.viniss.todo.service.exception.TodoListNotFoundException
import com.viniss.todo.service.exception.TaskNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.converter.HttpMessageNotReadableException
import java.util.UUID

class ApiExceptionHandlerTest {

    private val handler = ApiExceptionHandler()

    @Test
    fun `should map IllegalArgumentException to bad request response`() {
        val response = handler.handleIllegalArgument(IllegalArgumentException("Invalid payload"))

        assertThat(response.message).isEqualTo("Invalid payload")
    }

    @Test
    fun `should map TodoListNotFoundException to not found response`() {
        val id = UUID.randomUUID()
        val response = handler.handleTodoListNotFound(TodoListNotFoundException(id))

        assertThat(response.message).isEqualTo("Todo list $id not found")
    }

    @Test
    fun `should map TaskNotFoundException to not found response`() {
        val id = UUID.randomUUID()
        val response = handler.handleTaskNotFound(TaskNotFoundException(id))

        assertThat(response.message).isEqualTo("Task with id $id not found")
    }

    @Test
    fun `should map HttpMessageNotReadableException to bad request response`() {
        val ex = HttpMessageNotReadableException("JSON parse error: Cannot deserialize value")
        val response = handler.handleHttpMessageNotReadable(ex)

        assertThat(response.message).contains("Invalid JSON format or invalid enum value")
    }
}
