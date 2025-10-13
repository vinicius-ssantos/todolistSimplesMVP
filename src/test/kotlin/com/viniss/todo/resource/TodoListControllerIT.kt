package com.viniss.todo.resource

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.viniss.todo.api.dto.TaskResponse
import com.viniss.todo.api.dto.TodoListResponse
import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.repo.TaskRepository
import com.viniss.todo.repo.TodoListRepository
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
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class TodoListControllerIT(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val todoListRepository: TodoListRepository,
    @Autowired private val taskRepository: TaskRepository
) {

    private lateinit var primaryList: TodoListEntity

    @BeforeEach
    fun setUp() {
        taskRepository.deleteAll()
        todoListRepository.deleteAll()

        primaryList = todoListRepository.save(TodoListEntity(name = "Projetos"))
        todoListRepository.save(TodoListEntity(name = "Mercado"))

        val firstTask = TaskEntity(
            list = primaryList,
            title = "Criar estrutura do projeto",
            position = 0
        )
        taskRepository.save(firstTask)
    }

    @Test
    fun `should list all todo lists with their tasks`() {
        val result = mockMvc.get("/v1/lists")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(
            result.response.contentAsString,
            object : TypeReference<List<TodoListResponse>>() {}
        )

        assertThat(response).hasSize(2)
        val primary = response.first { it.name == "Projetos" }
        assertThat(primary.tasks).hasSize(1)
        assertThat(primary.tasks.first().title).isEqualTo("Criar estrutura do projeto")
    }

    @Test
    fun `should create a todo list`() {
        val payload = """{"name":"Estudos"}"""

        val result = mockMvc.post("/v1/lists") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, TodoListResponse::class.java)

        assertThat(response.id).isNotNull()
        assertThat(response.name).isEqualTo("Estudos")
        assertThat(response.tasks).isEmpty()

        assertThat(todoListRepository.count()).isEqualTo(3)
    }

    @Test
    fun `should create a task with default position`() {
        val payload = """{"title":"Criar endpoint POST"}"""

        val result = mockMvc.post("/v1/lists/${primaryList.id}/tasks") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, TaskResponse::class.java)

        assertThat(response.title).isEqualTo("Criar endpoint POST")
        assertThat(response.position).isEqualTo(1)

        val tasks = taskRepository.findByListIdOrderByPositionAsc(primaryList.id)
        assertThat(tasks).hasSize(2)
    }

    @Test
    fun `should return bad request when creating list with blank name`() {
        val payload = """{"name":"   "}"""

        mockMvc.post("/v1/lists") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Todo list name must not be blank") }
        }
    }

    @Test
    fun `should return bad request when creating task with blank title`() {
        val payload = """{"title":"    "}"""

        mockMvc.post("/v1/lists/${primaryList.id}/tasks") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Task title must not be blank") }
        }
    }

    @Test
    fun `should return not found when list does not exist`() {
        val listId = UUID.randomUUID()
        val payload = """{"title":"Nova task"}"""

        mockMvc.post("/v1/lists/$listId/tasks") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("Todo list $listId not found") }
        }
    }

    @Test
    fun `should reject tasks with duplicated position`() {
        val payload = """{"title":"Duplicada","position":0}"""

        mockMvc.post("/v1/lists/${primaryList.id}/tasks") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Task position 0 is already in use") }
        }
    }
}
