package com.viniss.todo.resource

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.viniss.todo.api.dto.TaskResponse
import com.viniss.todo.api.dto.TodoListResponse
import com.viniss.todo.config.TestMockMvcConfig
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
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.*
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@Import(TestMockMvcConfig::class)
class TodoListControllerIT(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val todoListRepository: TodoListRepository,
    @Autowired private val taskRepository: TaskRepository
) {

    private lateinit var primaryList: TodoListEntity
    private lateinit var firstTask: TaskEntity

    @BeforeEach
    fun setUp() {
        taskRepository.deleteAll()
        todoListRepository.deleteAll()

        primaryList = todoListRepository.save(TodoListEntity(name = "Projetos"))
        todoListRepository.save(TodoListEntity(name = "Mercado"))

        firstTask = TaskEntity(
            list = primaryList,
            title = "Criar estrutura do projeto",
            position = 0
        )
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

    // ========== GET BY ID TESTS ==========

    @Test
    fun `should get todo list by id`() {
        val result = mockMvc.get("/v1/lists/${primaryList.id}")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, TodoListResponse::class.java)

        assertThat(response.id).isEqualTo(primaryList.id)
        assertThat(response.name).isEqualTo("Projetos")
        assertThat(response.tasks).hasSize(1)
        assertThat(response.tasks.first().title).isEqualTo("Criar estrutura do projeto")
    }

    @Test
    fun `should get task by id`() {
        val result = mockMvc.get("/v1/lists/${primaryList.id}/tasks/${firstTask.id}")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, TaskResponse::class.java)

        assertThat(response.id).isEqualTo(firstTask.id)
        assertThat(response.title).isEqualTo("Criar estrutura do projeto")
        assertThat(response.position).isEqualTo(0)
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
        val otherList = todoListRepository.save(TodoListEntity(name = "Outra Lista"))
        val otherTask = taskRepository.save(TaskEntity(
            list = otherList,
            title = "Task de outra lista",
            position = 0
        ))

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
        assertThat(updatedList.name).isEqualTo("Projetos Atualizados")
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
        assertThat(updatedTask.title).isEqualTo("Task Atualizada")
        assertThat(updatedTask.notes).isEqualTo("Notas atualizadas")
        assertThat(updatedTask.priority).isEqualTo(com.viniss.todo.domain.Priority.HIGH)
        assertThat(updatedTask.status).isEqualTo(com.viniss.todo.domain.Status.IN_PROGRESS)
        assertThat(updatedTask.dueDate).isEqualTo(java.time.LocalDate.of(2024, 12, 31))
        assertThat(updatedTask.position).isEqualTo(5)
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
        assertThat(updatedTask.title).isEqualTo("Apenas título atualizado")
        assertThat(updatedTask.status).isEqualTo(com.viniss.todo.domain.Status.DONE)
        // Other fields should remain unchanged
        assertThat(updatedTask.position).isEqualTo(0)
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
            jsonPath("$.message") { value(org.hamcrest.Matchers.containsString("Invalid JSON format or invalid enum value")) }
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
        val secondTask = taskRepository.save(TaskEntity(
            list = primaryList,
            title = "Segunda task",
            position = 1
        ))

        mockMvc.delete("/v1/lists/${primaryList.id}")
            .andExpect {
                status { isNoContent() }
            }

        assertThat(todoListRepository.findById(primaryList.id)).isEmpty
        assertThat(taskRepository.findById(firstTask.id)).isEmpty
        assertThat(taskRepository.findById(secondTask.id)).isEmpty
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
        val otherList = todoListRepository.save(TodoListEntity(name = "Outra Lista"))
        val otherTask = taskRepository.save(TaskEntity(
            list = otherList,
            title = "Task de outra lista",
            position = 0
        ))

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
        val task2 = taskRepository.save(TaskEntity(
            list = primaryList,
            title = "Task 2",
            position = 1
        ))
        val task3 = taskRepository.save(TaskEntity(
            list = primaryList,
            title = "Task 3",
            position = 2
        ))

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

        assertThat(updatedFirstTask.position).isEqualTo(2)
        assertThat(updatedTask2.position).isEqualTo(0) // shifted down
        assertThat(updatedTask3.position).isEqualTo(1) // shifted down
    }

    @Test
    fun `should handle task creation with automatic position assignment`() {
        // Create tasks with specific positions
        taskRepository.save(TaskEntity(
            list = primaryList,
            title = "Task 1",
            position = 0
        ))
        taskRepository.save(TaskEntity(
            list = primaryList,
            title = "Task 2",
            position = 1
        ))

        // Create task without position (should auto-assign)
        val payload = """{"title":"Task Auto Position"}"""

        val result = mockMvc.post("/v1/lists/${primaryList.id}/tasks") {
            contentType = MediaType.APPLICATION_JSON
            content = payload
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, TaskResponse::class.java)
        assertThat(response.position).isEqualTo(2) // Should be next available position
    }

    // ========== EDGE CASES TESTS ==========

    @Test
    fun `should handle empty lists gracefully`() {
        val emptyList = todoListRepository.save(TodoListEntity(name = "Lista Vazia"))

        val result = mockMvc.get("/v1/lists/${emptyList.id}")
            .andExpect {
                status { isOk() }
            }
            .andReturn()

        val response = objectMapper.readValue(result.response.contentAsString, TodoListResponse::class.java)
        assertThat(response.tasks).isEmpty()
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
        assertThat(updatedTask.title).isEqualTo("Task com campos null")
        // Other fields should remain unchanged since they were null
        assertThat(updatedTask.position).isEqualTo(0)
    }
}
