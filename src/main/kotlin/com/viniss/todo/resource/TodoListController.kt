package com.viniss.todo.resource

import com.viniss.todo.api.dto.*
import com.viniss.todo.api.mapper.RequestMapper.toCommand
import com.viniss.todo.api.mapper.ResponseMapper.toResponse
import com.viniss.todo.service.port.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import com.viniss.todo.api.param.StatusParamMapper
import com.viniss.todo.domain.Priority

@RestController
@RequestMapping("/api/v1/lists")
class TodoListController(
    private val listQueryUseCase: ListQueryUseCase,
    private val createTodoListUseCase: CreateTodoListUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTodoListUseCase: UpdateTodoListUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val getTaskUseCase: GetTaskUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val listTasksUseCase: ListTasksUseCase
) {
    @GetMapping
    fun getAll(): List<TodoListResponse> =
        listQueryUseCase.findAllWithTasks().map { it.toResponse() }

    @GetMapping("/{listId}")
    fun getById(@PathVariable listId: UUID): TodoListResponse =
        listQueryUseCase.findById(listId).toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createList(@RequestBody request: CreateTodoListRequest): TodoListResponse =
        createTodoListUseCase.create(request.toCommand()).toResponse()

    @PostMapping("/{listId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTask(
        @PathVariable listId: UUID,
        @RequestBody request: CreateTaskRequest
    ): TaskResponse =
        createTaskUseCase.create(listId, request.toCommand()).toResponse()

    @PatchMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateList(@PathVariable listId: UUID, @RequestBody body: UpdateTodoListRequest): ResponseEntity<Void> {
        updateTodoListUseCase.update(listId, body.toCommand())
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{listId}/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateTask(
        @PathVariable listId: UUID,
        @PathVariable taskId: UUID,
        @RequestBody request: UpdateTaskRequest
    ): ResponseEntity<Void> {
        updateTaskUseCase.update(listId, taskId, request.toCommand())
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{listId}/tasks/{taskId}")
    fun getTaskById(
        @PathVariable listId: UUID,
        @PathVariable taskId: UUID
    ): TaskResponse =
        getTaskUseCase.findById(listId, taskId).toResponse()

    @DeleteMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteList(@PathVariable listId: UUID): ResponseEntity<Void> {
        deleteTodoListUseCase.delete(listId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{listId}/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTask(
        @PathVariable listId: UUID,
        @PathVariable taskId: UUID
    ): ResponseEntity<Void> {
        deleteTaskUseCase.delete(listId, taskId)
        return ResponseEntity.noContent().build()
    }

    // paginação + filtros de tarefas de uma lista
    @GetMapping("/{listId}/tasks")
    fun listTasks(
        @PathVariable listId: UUID,
        @RequestParam(required = false, defaultValue = "all") due: String?,
        @RequestParam(required = false, defaultValue = "all") status: String?,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) priority: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<TaskResponse>> {
        val filters = TaskFilters(
            due = DueFilter.fromParam(due),
            statuses = StatusParamMapper.from(status),
            search = search,
            priority = priority?.let { runCatching { Priority.valueOf(it) }.getOrNull() }
        )
        val page = listTasksUseCase.listByFilters(listId, filters, pageable).map { it.toResponse() }
        return ResponseEntity.ok(page)
    }


}
