package com.viniss.todo.it

import com.fasterxml.jackson.databind.ObjectMapper
import com.viniss.todo.auth.AppUserRepository
import com.viniss.todo.auth.AuthResponse
import com.viniss.todo.repo.TaskRepository
import com.viniss.todo.repo.TodoListRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
abstract class AuthControllerIT {

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var objectMapper: ObjectMapper
    @Autowired protected lateinit var todoListRepository: TodoListRepository
    @Autowired protected lateinit var taskRepository: TaskRepository
    @Autowired protected lateinit var appUserRepository: AppUserRepository

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

        Assertions.assertThat(registerToken).isNotBlank()


        val unauthorized = mockMvc.get("/v1/lists").andReturn()
        Assertions.assertThat(unauthorized.response.status).isIn(401, 403)

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

        Assertions.assertThat(loginToken).isNotBlank()

        mockMvc.get("/v1/lists") {
            header("Authorization", "Bearer $loginToken")
        }.andExpect {
            status { isOk() }
        }
    }
}
