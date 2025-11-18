package com.viniss.todo.resource

import com.viniss.todo.api.dto.*
import com.viniss.todo.api.mapper.RequestMapper.toCommand
import com.viniss.todo.api.mapper.ResponseMapper.toResponse
import com.viniss.todo.service.port.*
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/lists")
class TodoListController(
    private val listQueryUseCase: ListQueryUseCase,
    private val createTodoListUseCase: CreateTodoListUseCase,
    private val createTaskUseCase: CreateTaskUseCase,
    private val updateTodoListUseCase: UpdateTodoListUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val getTaskUseCase: GetTaskUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) {
    @GetMapping
    fun getAll(): List<TodoListResponse> =
        listQueryUseCase.findAllWithTasks().map { it.toResponse() }

    @GetMapping("/{listId}")
    fun getById(@PathVariable listId: UUID): TodoListResponse =
        listQueryUseCase.findById(listId).toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createList(@Valid @RequestBody request: CreateTodoListRequest): TodoListResponse =
        createTodoListUseCase.create(request.toCommand()).toResponse()

    @PostMapping("/{listId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    fun createTask(
        @PathVariable listId: UUID,
        @Valid @RequestBody request: CreateTaskRequest
    ): TaskResponse =
        createTaskUseCase.create(listId, request.toCommand()).toResponse()

    @PatchMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateList(@PathVariable listId: UUID, @Valid @RequestBody body: UpdateTodoListRequest): ResponseEntity<Void> {
        updateTodoListUseCase.update(listId, body.toCommand())
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{listId}/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateTask(
        @PathVariable listId: UUID,
        @PathVariable taskId: UUID,
        @Valid @RequestBody request: UpdateTaskRequest
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

}
