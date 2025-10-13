package com.viniss.todo.resource

import com.viniss.todo.service.exception.TodoListNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
        val response = handler.handleNotFound(TodoListNotFoundException(id))

        assertThat(response.message).isEqualTo("Todo list $id not found")
    }
}
