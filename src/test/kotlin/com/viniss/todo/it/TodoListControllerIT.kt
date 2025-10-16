package com.viniss.todo.it

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.viniss.todo.api.dto.TaskResponse
import com.viniss.todo.api.dto.TodoListResponse
import com.viniss.todo.auth.AppUserEntity
import com.viniss.todo.auth.AppUserRepository
import com.viniss.todo.config.TestMockMvcConfig
import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.repo.TaskRepository
import com.viniss.todo.repo.TodoListRepository
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDate
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "00000000-0000-0000-0000-000000000001")
@Import(TestMockMvcConfig::class)
class TodoListControllerIT(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val todoListRepository: TodoListRepository,
    @Autowired private val taskRepository: TaskRepository,
    @Autowired private val appUserRepository: AppUserRepository
) {

    private lateinit var primaryList: TodoListEntity
    private lateinit var firstTask: TaskEntity
    lateinit var seedUserId: UUID

    @BeforeEach
    fun setUp() {
        taskRepository.deleteAll()
        todoListRepository.deleteAll()

        seedUserId = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val seedUser = appUserRepository.save(
            AppUserEntity(
                id = seedUserId,                     // <- fixa o id
                email = "it@example.com",
                passwordHash = "noop"
            )
        )

        primaryList = todoListRepository.save(
            TodoListEntity(name = "Projetos").apply { userId = seedUserId }
        )
        todoListRepository.save(
            TodoListEntity(name = "Mercado").apply { userId = seedUserId }
        )

        firstTask = TaskEntity(
            list = primaryList,
            title = "Criar estrutura do projeto",
            position = 0
        ).apply { userId = seedUserId }             // <- idem aqui
        firstTask = taskRepository.save(firstTask)
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

        Assertions.assertThat(response).hasSize(2)
        val primary = response.first { it.name == "Projetos" }
        Assertions.assertThat(primary.tasks).hasSize(1)
        Assertions.assertThat(primary.tasks.first().title).isEqualTo("Criar estrutura do projeto")
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

        Assertions.assertThat(response.id).isNotNull()
        Assertions.assertThat(response.name).isEqualTo("Estudos")
        Assertions.assertThat(response.tasks).isEmpty()

        Assertions.assertThat(todoListRepository.count()).isEqualTo(3)
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

        Assertions.assertThat(response.title).isEqualTo("Criar endpoint POST")
        Assertions.assertThat(response.position).isEqualTo(1)

        val tasks = taskRepository.findByListIdOrderByPositionAsc(primaryList.id)
        Assertions.assertThat(tasks).hasSize(2)
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

    // ========== GET BY ID TESTS ==========

    @Test
    fun `should get todo list by id`() {
        val result = mockMvc.get("/v1/lists/${primaryList.id}")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, TodoListResponse::class.java)

        Assertions.assertThat(response.id).isEqualTo(primaryList.id)
        Assertions.assertThat(response.name).isEqualTo("Projetos")
        Assertions.assertThat(response.tasks).hasSize(1)
        Assertions.assertThat(response.tasks.first().title).isEqualTo("Criar estrutura do projeto")
    }

    @Test
    fun `should get task by id`() {
        val result = mockMvc.get("/v1/lists/${primaryList.id}/tasks/${firstTask.id}")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, TaskResponse::class.java)

        Assertions.assertThat(response.id).isEqualTo(firstTask.id)
        Assertions.assertThat(response.title).isEqualTo("Criar estrutura do projeto")
        Assertions.assertThat(response.position).isEqualTo(0)
    }

    @Test
    fun `should return not found when getting non-existent list`() {
        val listId = UUID.randomUUID()

        mockMvc.get("/v1/lists/$listId")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Todo list $listId not found") }
            }
    }

    @Test
    fun `should return not found when getting non-existent task`() {
        val taskId = UUID.randomUUID()

        mockMvc.get("/v1/lists/${primaryList.id}/tasks/$taskId")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Task with id $taskId not found") }
            }
    }

    @Test
    fun `should return not found when task does not belong to list`() {
        val otherList = todoListRepository.save(TodoListEntity(name = "Outra Lista").apply { userId = seedUserId })
        val otherTask = taskRepository.save(
            TaskEntity(
                list = otherList,
                title = "Task de outra lista",
                position = 0
            ).apply { userId = seedUserId })

        mockMvc.get("/v1/lists/${primaryList.id}/tasks/${otherTask.id}")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Task with id ${otherTask.id} not found") }
            }
    }

    // ========== UPDATE TESTS ==========

    @Test
    fun `should update todo list name`() {
        val payload = """{"name":"Projetos Atualizados"}"""

        mockMvc.patch("/v1/lists/${primaryList.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isNoContent() }
        }

        val updatedList = todoListRepository.findById(primaryList.id).get()
        Assertions.assertThat(updatedList.name).isEqualTo("Projetos Atualizados")
    }

    @Test
    fun `should update task with all fields`() {
        val payload = """{
            "title": "Task Atualizada",
            "notes": "Notas atualizadas",
            "priority": "HIGH",
            "status": "IN_PROGRESS",
            "dueDate": "2024-12-31",
            "position": 5
        }"""

        mockMvc.patch("/v1/lists/${primaryList.id}/tasks/${firstTask.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isNoContent() }
        }

        val updatedTask = taskRepository.findById(firstTask.id).get()
        Assertions.assertThat(updatedTask.title).isEqualTo("Task Atualizada")
        Assertions.assertThat(updatedTask.notes).isEqualTo("Notas atualizadas")
        Assertions.assertThat(updatedTask.priority).isEqualTo(Priority.HIGH)
        Assertions.assertThat(updatedTask.status).isEqualTo(Status.IN_PROGRESS)
        Assertions.assertThat(updatedTask.dueDate).isEqualTo(LocalDate.of(2024, 12, 31))
        Assertions.assertThat(updatedTask.position).isEqualTo(5)
    }

    @Test
    fun `should update task with partial fields`() {
        val payload = """{
            "title": "Apenas título atualizado",
            "status": "DONE"
        }"""

        mockMvc.patch("/v1/lists/${primaryList.id}/tasks/${firstTask.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isNoContent() }
        }

        val updatedTask = taskRepository.findById(firstTask.id).get()
        Assertions.assertThat(updatedTask.title).isEqualTo("Apenas título atualizado")
        Assertions.assertThat(updatedTask.status).isEqualTo(Status.DONE)
        // Other fields should remain unchanged
        Assertions.assertThat(updatedTask.position).isEqualTo(0)
    }

    @Test
    fun `should return bad request when updating list with blank name`() {
        val payload = """{"name":"   "}"""

        mockMvc.patch("/v1/lists/${primaryList.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Todo list name must not be blank") }
        }
    }

    @Test
    fun `should return bad request when updating task with blank title`() {
        val payload = """{"title":"   "}"""

        mockMvc.patch("/v1/lists/${primaryList.id}/tasks/${firstTask.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Task title must not be blank") }
        }
    }

    @Test
    fun `should return bad request when updating task with invalid status`() {
        val payload = """{"status":"INVALID_STATUS"}"""

        mockMvc.patch("/v1/lists/${primaryList.id}/tasks/${firstTask.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(Matchers.containsString("Invalid JSON format or invalid enum value")) }
        }
    }

    @Test
    fun `should return bad request when updating task with negative position`() {
        val payload = """{"position":-1}"""

        mockMvc.patch("/v1/lists/${primaryList.id}/tasks/${firstTask.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Task position must be zero or positive") }
        }
    }

    // ========== DELETE TESTS ==========

    @Test
    fun `should delete task`() {
        mockMvc.delete("/v1/lists/${primaryList.id}/tasks/${firstTask.id}")
            .andExpect {
                status { isNoContent() }
            }
    }

    @Test
    fun `should delete todo list and all its tasks`() {
        // Create another task in the same list
        val secondTask = taskRepository.save(
            TaskEntity(
                list = primaryList,
                title = "Segunda task",
                position = 1
            ).apply { userId = seedUserId })

        mockMvc.delete("/v1/lists/${primaryList.id}")
            .andExpect {
                status { isNoContent() }
            }

        Assertions.assertThat(todoListRepository.findById(primaryList.id)).isEmpty
        Assertions.assertThat(taskRepository.findById(firstTask.id)).isEmpty
        Assertions.assertThat(taskRepository.findById(secondTask.id)).isEmpty
    }

    @Test
    fun `should return not found when deleting non-existent list`() {
        val listId = UUID.randomUUID()

        mockMvc.delete("/v1/lists/$listId")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Todo list $listId not found") }
            }
    }

    @Test
    fun `should return not found when deleting non-existent task`() {
        val taskId = UUID.randomUUID()

        mockMvc.delete("/v1/lists/${primaryList.id}/tasks/$taskId")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Task with id $taskId not found") }
            }
    }

    @Test
    fun `should return not found when deleting task from wrong list`() {
        val otherList = todoListRepository.save(TodoListEntity(name = "Outra Lista").apply { userId = seedUserId })
        val otherTask = taskRepository.save(
            TaskEntity(
                list = otherList,
                title = "Task de outra lista",
                position = 0
            ).apply { userId = seedUserId })

        mockMvc.delete("/v1/lists/${primaryList.id}/tasks/${otherTask.id}")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Task with id ${otherTask.id} not found") }
            }
    }

    // ========== POSITION REORGANIZATION TESTS ==========

    @Test
    fun `should reorganize positions when updating task position`() {
        // Create multiple tasks
        val task2 = taskRepository.save(
            TaskEntity(
                list = primaryList,
                title = "Task 2",
                position = 1
            ).apply { userId = seedUserId })
        val task3 = taskRepository.save(
            TaskEntity(
                list = primaryList,
                title = "Task 3",
                position = 2
            ).apply { userId = seedUserId })

        // Move first task to position 2 (should shift others)
        val payload = """{"position":2}"""

        mockMvc.patch("/v1/lists/${primaryList.id}/tasks/${firstTask.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isNoContent() }
        }

        // Verify positions were reorganized
        val updatedFirstTask = taskRepository.findById(firstTask.id).get()
        val updatedTask2 = taskRepository.findById(task2.id).get()
        val updatedTask3 = taskRepository.findById(task3.id).get()

        Assertions.assertThat(updatedFirstTask.position).isEqualTo(2)
        Assertions.assertThat(updatedTask2.position).isEqualTo(0) // shifted down
        Assertions.assertThat(updatedTask3.position).isEqualTo(1) // shifted down
    }

    @Test
    fun `should handle task creation with automatic position assignment`() {
        // Create tasks with specific positions
        taskRepository.save(
            TaskEntity(
                list = primaryList,
                title = "Task 1",
                position = 0
            ).apply { userId = seedUserId })
        taskRepository.save(
            TaskEntity(
                list = primaryList,
                title = "Task 2",
                position = 1
            ).apply { userId = seedUserId })

        // Create task without position (should auto-assign)
        val payload = """{"title":"Task Auto Position"}"""

        val result = mockMvc.post("/v1/lists/${primaryList.id}/tasks") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, TaskResponse::class.java)
        Assertions.assertThat(response.position).isEqualTo(2) // Should be next available position
    }

    // ========== EDGE CASES TESTS ==========

    @Test
    fun `should handle empty lists gracefully`() {
        val emptyList = todoListRepository.save(TodoListEntity(name = "Lista Vazia").apply { userId = seedUserId })

        val result = mockMvc.get("/v1/lists/${emptyList.id}")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, TodoListResponse::class.java)
        Assertions.assertThat(response.tasks).isEmpty()
    }

    @Test
    fun `should handle null values in update requests`() {
        val payload = """{
            "title": "Task com campos null",
            "notes": null,
            "priority": null,
            "status": null,
            "dueDate": null,
            "position": null
        }"""

        mockMvc.patch("/v1/lists/${primaryList.id}/tasks/${firstTask.id}") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isNoContent() }
        }

        val updatedTask = taskRepository.findById(firstTask.id).get()
        Assertions.assertThat(updatedTask.title).isEqualTo("Task com campos null")
        // Other fields should remain unchanged since they were null
        Assertions.assertThat(updatedTask.position).isEqualTo(0)
    }
}