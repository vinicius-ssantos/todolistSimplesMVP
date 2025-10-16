package com.viniss.todo.it

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
abstract class P0AuthorizationIT {

    @Autowired protected lateinit var mockMvc: MockMvc
    @Autowired protected lateinit var mapper: ObjectMapper

    private lateinit var tokenA: String
    private lateinit var tokenB: String
    private lateinit var listId: UUID
    private lateinit var taskId: UUID

    @BeforeEach
    fun setUp() {
        tokenA = register("a${rand()}@ex.com", "secret123")
        tokenB = register("b${rand()}@ex.com", "secret123")
        listId = createList(tokenA, name = "Lista A")
        taskId = createTask(tokenA, listId, title = "Task A1")
    }

    @Test
    @DisplayName("P0: B não pode GET lista de A → 404")
    fun bCannotGetListOfA() {
        mockMvc.perform(
            MockMvcRequestBuilders.get("/v1/lists/$listId")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DisplayName("P0: B não pode PATCH lista de A → 404")
    fun bCannotPatchListOfA() {
        val updateListJson = """{ "name": "NOME HACK" }"""
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/v1/lists/$listId")
                .header("Authorization", "Bearer $tokenB")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateListJson)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DisplayName("P0: B não pode DELETE lista de A → 404")
    fun bCannotDeleteListOfA() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/v1/lists/$listId")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DisplayName("P0: B não pode acessar task de A (GET/PATCH/DELETE) → 404")
    fun bCannotAccessTaskOfA_getPatchDelete() {
        // GET task de A
        mockMvc.perform(
            MockMvcRequestBuilders.get("/v1/lists/$listId/tasks/$taskId")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)

        // PATCH task de A
        val patchTaskJson = """{ "title": "TÍTULO HACK" }"""
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/v1/lists/$listId/tasks/$taskId")
                .header("Authorization", "Bearer $tokenB")
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchTaskJson)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)

        // DELETE task de A
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/v1/lists/$listId/tasks/$taskId")
                .header("Authorization", "Bearer $tokenB")
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @DisplayName("P0: B não pode criar task em listId de A → 404 da lista")
    fun bCannotCreateTaskInsideListOfA() {
        val createTaskJson = """{ "title": "Task de B na lista de A" }"""
        mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/lists/$listId/tasks")
                .header("Authorization", "Bearer $tokenB")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createTaskJson)
        ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    // ----------------- helpers -----------------

    private fun register(email: String, password: String): String {
        val body = """{"email":"$email","password":"$password"}"""
        val mvcRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val json = mapper.readTree(mvcRes.response.contentAsString)
        return json["token"].asText()
    }

    private fun createList(token: String, name: String): UUID {
        val mvcRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/lists")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"$name"}""")
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andReturn()

        val json = mapper.readTree(mvcRes.response.contentAsString)
        return UUID.fromString(json["id"].asText())
    }

    private fun createTask(token: String, listId: UUID, title: String): UUID {
        val body = """{"title":"$title"}"""
        val mvcRes = mockMvc.perform(
            MockMvcRequestBuilders.post("/v1/lists/$listId/tasks")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andReturn()

        val json: JsonNode = mapper.readTree(mvcRes.response.contentAsString)
        return UUID.fromString(json["id"].asText())
    }

    private fun rand() = System.nanoTime().toString(16)
}
