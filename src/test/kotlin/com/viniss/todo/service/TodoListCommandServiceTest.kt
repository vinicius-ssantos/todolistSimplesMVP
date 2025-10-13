package com.viniss.todo.service

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import com.viniss.todo.service.exception.TodoListNotFoundException
import com.viniss.todo.service.model.CreateTaskCommand
import com.viniss.todo.service.model.CreateTodoListCommand
import com.viniss.todo.service.model.TaskCreationData
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import com.viniss.todo.service.port.TodoListReadRepository
import com.viniss.todo.service.port.TodoListWriteRepository
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TodoListCommandServiceTest {

    @MockK(relaxed = true)
    lateinit var writeRepository: TodoListWriteRepository

    @MockK
    lateinit var readRepository: TodoListReadRepository

    private lateinit var service: TodoListCommandService

    private val listId = UUID.randomUUID()
    private val baseList = TodoListView(
        id = listId,
        name = "Projetos",
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        tasks = emptyList()
    )

    @BeforeEach
    fun setUp() {
        service = TodoListCommandService(writeRepository, readRepository)
    }

    @Test
    fun `should trim name and delegate creation`() {
        val command = CreateTodoListCommand("  Lista Nova  ")
        val created = baseList.copy(name = "Lista Nova")

        every { writeRepository.createList("Lista Nova") } returns created

        val response = service.create(command)

        assertThat(response.name).isEqualTo("Lista Nova")
        verify { writeRepository.createList("Lista Nova") }
    }

    @Test
    fun `should reject blank list name`() {
        val command = CreateTodoListCommand("   ")

        assertThatThrownBy { service.create(command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Todo list name must not be blank")
    }

    @Test
    fun `should default position and task attributes when list has tasks`() {
        val command = CreateTaskCommand(title = "Nova task")
        val existingTasks = listOf(TaskView(
            id = UUID.randomUUID(),
            title = "Existente",
            notes = null,
            priority = Priority.MEDIUM,
            status = Status.OPEN,
            dueDate = null,
            position = 3,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ))
        val listWithTasks = baseList.copy(tasks = existingTasks)

        val capturedTask = slot<TaskCreationData>()

        every { readRepository.findByIdWithTasks(listId) } returns listWithTasks
        every { writeRepository.addTask(listId, capture(capturedTask)) } returns TaskView(
            id = UUID.randomUUID(),
            title = "Nova task",
            notes = null,
            priority = Priority.MEDIUM,
            status = Status.OPEN,
            dueDate = null,
            position = 4,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        service.create(listId, command)

        with(capturedTask.captured) {
            assertThat(title).isEqualTo("Nova task")
            assertThat(position).isEqualTo(4)
            assertThat(priority).isEqualTo(Priority.MEDIUM)
            assertThat(status).isEqualTo(Status.OPEN)
        }
    }

    @Test
    fun `should apply custom position priority and status`() {
        val command = CreateTaskCommand(
            title = "Customizada",
            priority = Priority.HIGH,
            status = Status.DONE,
            position = 7
        )
        every { readRepository.findByIdWithTasks(listId) } returns baseList

        val capturedTask = slot<TaskCreationData>()
        every { writeRepository.addTask(listId, capture(capturedTask)) } returns TaskView(
            id = UUID.randomUUID(),
            title = "Customizada",
            notes = null,
            priority = Priority.HIGH,
            status = Status.DONE,
            dueDate = null,
            position = 7,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        service.create(listId, command)

        with(capturedTask.captured) {
            assertThat(position).isEqualTo(7)
            assertThat(priority).isEqualTo(Priority.HIGH)
            assertThat(status).isEqualTo(Status.DONE)
        }
    }

    @Test
    fun `should reject blank task title`() {
        val command = CreateTaskCommand("    ")
        every { readRepository.findByIdWithTasks(listId) } returns baseList

        assertThatThrownBy { service.create(listId, command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Task title must not be blank")
    }

    @Test
    fun `should reject negative task position`() {
        val command = CreateTaskCommand(title = "Teste", position = -1)
        every { readRepository.findByIdWithTasks(listId) } returns baseList

        assertThatThrownBy { service.create(listId, command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Task position must be zero or positive")
    }

    @Test
    fun `should reject duplicated task position`() {
        val existingTasks = listOf(TaskView(
            id = UUID.randomUUID(),
            title = "Existente",
            notes = null,
            priority = Priority.MEDIUM,
            status = Status.OPEN,
            dueDate = null,
            position = 5,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        ))
        val listWithTasks = baseList.copy(tasks = existingTasks)
        val command = CreateTaskCommand(title = "Duplicada", position = 5)

        every { readRepository.findByIdWithTasks(listId) } returns listWithTasks

        assertThatThrownBy { service.create(listId, command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Task position 5 is already in use")
    }

    @Test
    fun `should throw when list not found`() {
        val command = CreateTaskCommand(title = "Nova task")
        every { readRepository.findByIdWithTasks(listId) } returns null

        assertThatThrownBy { service.create(listId, command) }
            .isInstanceOf(TodoListNotFoundException::class.java)
            .hasMessage("Todo list $listId not found")
    }
}
