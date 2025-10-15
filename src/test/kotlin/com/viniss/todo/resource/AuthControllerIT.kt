package com.viniss.todo.resource

import com.fasterxml.jackson.databind.ObjectMapper
import com.viniss.todo.auth.AuthResponse
import com.viniss.todo.auth.AppUserRepository
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.repo.TaskRepository
import com.viniss.todo.repo.TodoListRepository
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val todoListRepository: TodoListRepository,
    private val taskRepository: TaskRepository,
    private val appUserRepository: AppUserRepository
) {

    @BeforeEach
    fun cleanDatabase() {
        taskRepository.deleteAll()
        todoListRepository.deleteAll()
        appUserRepository.deleteAll()
    }

    @Test
    fun `user can register, login and access protected endpoints`() {
        val email = "test-${UUID.randomUUID()}@example.com"
        val password = "secret123"

        val registerResponse = mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val registerToken = objectMapper
            .readValue(registerResponse.response.contentAsString, AuthResponse::class.java)
            .token

        assertThat(registerToken).isNotBlank()

        todoListRepository.save(TodoListEntity(name = "Work"))

        val unauthorized = mockMvc.get("/v1/lists").andReturn()
        assertThat(unauthorized.response.status).isIn(401, 403)

        mockMvc.get("/v1/lists") {
            header("Authorization", "Bearer $registerToken")
        }.andExpect {
            status { isOk() }
        }

        val loginResponse = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val loginToken = objectMapper
            .readValue(loginResponse.response.contentAsString, AuthResponse::class.java)
            .token

        assertThat(loginToken).isNotBlank()

        mockMvc.get("/v1/lists") {
            header("Authorization", "Bearer $loginToken")
        }.andExpect {
            status { isOk() }
        }
    }
}
