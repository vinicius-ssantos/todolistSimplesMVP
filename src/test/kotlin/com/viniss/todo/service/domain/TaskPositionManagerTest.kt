package com.viniss.todo.service.domain

import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.repo.TaskRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class TaskPositionManagerTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var taskPositionManager: TaskPositionManager

    @BeforeEach
    fun setUp() {
        taskRepository = mockk(relaxed = true)
        taskPositionManager = DefaultTaskPositionManager(taskRepository)
    }

    @Test
    fun `should reorganize positions when moving task down`() {
        // Given
        val listId = UUID.randomUUID()
        val list = createTodoList(listId)

        val task0 = createTask(UUID.randomUUID(), "Task 0", 0, list)
        val task1 = createTask(UUID.randomUUID(), "Task 1", 1, list)
        val task2 = createTask(UUID.randomUUID(), "Task 2", 2, list)
        val task3 = createTask(UUID.randomUUID(), "Task 3", 3, list)

        val allTasks = listOf(task0, task1, task2, task3)

        every { taskRepository.findByListIdOrderByPositionAsc(listId) } returns allTasks

        // When: move task0 (position 0) to position 2
        taskPositionManager.reorganizePositions(list, task0, 2)

        // Then: tasks 1 and 2 should shift up
        assertEquals(0, task1.position) // was 1, shifted up
        assertEquals(1, task2.position) // was 2, shifted up
        assertEquals(3, task3.position) // unchanged

        verify { taskRepository.saveAll(match<List<TaskEntity>> { it.size == 3 && it.contains(task1) && it.contains(task2) && it.contains(task3) }) }
    }

    @Test
    fun `should reorganize positions when moving task up`() {
        // Given
        val listId = UUID.randomUUID()
        val list = createTodoList(listId)

        val task0 = createTask(UUID.randomUUID(), "Task 0", 0, list)
        val task1 = createTask(UUID.randomUUID(), "Task 1", 1, list)
        val task2 = createTask(UUID.randomUUID(), "Task 2", 2, list)
        val task3 = createTask(UUID.randomUUID(), "Task 3", 3, list)

        val allTasks = listOf(task0, task1, task2, task3)

        every { taskRepository.findByListIdOrderByPositionAsc(listId) } returns allTasks

        // When: move task3 (position 3) to position 1
        taskPositionManager.reorganizePositions(list, task3, 1)

        // Then: tasks 1 and 2 should shift down
        assertEquals(0, task0.position) // unchanged
        assertEquals(2, task1.position) // was 1, shifted down
        assertEquals(3, task2.position) // was 2, shifted down

        verify { taskRepository.saveAll(match<List<TaskEntity>> { it.size == 3 }) }
    }

    @Test
    fun `should not reorganize when position does not change`() {
        // Given
        val listId = UUID.randomUUID()
        val list = createTodoList(listId)

        val task0 = createTask(UUID.randomUUID(), "Task 0", 0, list)
        val task1 = createTask(UUID.randomUUID(), "Task 1", 1, list)

        val allTasks = listOf(task0, task1)

        every { taskRepository.findByListIdOrderByPositionAsc(listId) } returns allTasks

        // When: move task0 to same position
        taskPositionManager.reorganizePositions(list, task0, 0)

        // Then: no changes should be made
        assertEquals(0, task0.position)
        assertEquals(1, task1.position)

        // saveAll should not be called or called with empty list
        verify(exactly = 0) { taskRepository.saveAll(match<List<TaskEntity>> { it.isNotEmpty() }) }
    }

    @Test
    fun `should throw exception for negative position`() {
        // Given
        val listId = UUID.randomUUID()
        val list = createTodoList(listId)
        val task0 = createTask(UUID.randomUUID(), "Task 0", 0, list)

        every { taskRepository.findByListIdOrderByPositionAsc(listId) } returns listOf(task0)

        // When/Then
        val exception = assertThrows<IllegalArgumentException> {
            taskPositionManager.reorganizePositions(list, task0, -1)
        }

        assertTrue(exception.message?.contains("negative") == true)
    }

    @Test
    fun `should normalize positions sequentially`() {
        // Given
        val listId = UUID.randomUUID()
        val list = createTodoList(listId)

        val task0 = createTask(UUID.randomUUID(), "Task 0", 5, list)  // irregular positions
        val task1 = createTask(UUID.randomUUID(), "Task 1", 10, list)
        val task2 = createTask(UUID.randomUUID(), "Task 2", 15, list)

        val allTasks = listOf(task0, task1, task2)

        every { taskRepository.findByListIdOrderByPositionAsc(listId) } returns allTasks
        every { taskRepository.saveAll(any<List<TaskEntity>>()) } returns allTasks

        // When
        val result = taskPositionManager.normalizePositions(listId)

        // Then: positions should be 0, 1, 2
        assertEquals(0, task0.position)
        assertEquals(1, task1.position)
        assertEquals(2, task2.position)

        verify { taskRepository.saveAll(match<List<TaskEntity>> { it.size == 3 }) }
        assertEquals(3, result.size)
    }

    @Test
    fun `should handle empty list when normalizing positions`() {
        // Given
        val listId = UUID.randomUUID()

        every { taskRepository.findByListIdOrderByPositionAsc(listId) } returns emptyList()

        // When
        val result = taskPositionManager.normalizePositions(listId)

        // Then
        assertTrue(result.isEmpty())
        verify(exactly = 0) { taskRepository.saveAll(any<List<TaskEntity>>()) }
    }

    @Test
    fun `should handle moving task to position beyond list size`() {
        // Given
        val listId = UUID.randomUUID()
        val list = createTodoList(listId)

        val task0 = createTask(UUID.randomUUID(), "Task 0", 0, list)
        val task1 = createTask(UUID.randomUUID(), "Task 1", 1, list)
        val task2 = createTask(UUID.randomUUID(), "Task 2", 2, list)

        val allTasks = listOf(task0, task1, task2)

        every { taskRepository.findByListIdOrderByPositionAsc(listId) } returns allTasks

        // When: try to move task0 to position 100 (beyond list size)
        taskPositionManager.reorganizePositions(list, task0, 100)

        // Then: should clamp to max position (2)
        // Tasks 1 and 2 should shift up
        assertEquals(0, task1.position)
        assertEquals(1, task2.position)

        verify { taskRepository.saveAll(any<List<TaskEntity>>()) }
    }

    // Helper methods

    private fun createTodoList(id: UUID): TodoListEntity {
        return TodoListEntity(id = id, name = "Test List").apply {
            this.userId = UUID.randomUUID()
        }
    }

    private fun createTask(id: UUID, title: String, position: Int, list: TodoListEntity): TaskEntity {
        return TaskEntity(
            id = id,
            list = list,
            title = title,
            position = position,
            priority = Priority.MEDIUM,
            status = Status.OPEN
        ).apply {
            this.userId = UUID.randomUUID()
        }
    }
}
