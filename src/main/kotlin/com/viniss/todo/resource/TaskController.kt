package com.viniss.todo.resource

import com.viniss.todo.api.dto.CreateTaskRequest
import com.viniss.todo.api.dto.TaskResponse
import com.viniss.todo.api.dto.UpdateTaskRequest
import com.viniss.todo.api.mapper.RequestMapper.toCommand
import com.viniss.todo.api.mapper.ResponseMapper.toResponse
import com.viniss.todo.service.port.CreateTaskUseCase
import com.viniss.todo.service.port.DeleteTaskUseCase
import com.viniss.todo.service.port.GetTaskUseCase
import com.viniss.todo.service.port.UpdateTaskUseCase
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/lists/{listId}/tasks")
class TaskController(
    private val createTaskUseCase: CreateTaskUseCase,
    private val getTaskUseCase: GetTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTask(
        @PathVariable listId: UUID,
        @Valid @RequestBody request: CreateTaskRequest
    ): TaskResponse =
        createTaskUseCase.create(listId, request.toCommand()).toResponse()

    @GetMapping("/{taskId}")
    fun getTaskById(
        @PathVariable listId: UUID,
        @PathVariable taskId: UUID
    ): TaskResponse =
        getTaskUseCase.findById(listId, taskId).toResponse()

    @PatchMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateTask(
        @PathVariable listId: UUID,
        @PathVariable taskId: UUID,
        @Valid @RequestBody request: UpdateTaskRequest
    ): ResponseEntity<Void> {
        updateTaskUseCase.update(listId, taskId, request.toCommand())
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTask(
        @PathVariable listId: UUID,
        @PathVariable taskId: UUID
    ): ResponseEntity<Void> {
        deleteTaskUseCase.delete(listId, taskId)
        return ResponseEntity.noContent().build()
    }
}
