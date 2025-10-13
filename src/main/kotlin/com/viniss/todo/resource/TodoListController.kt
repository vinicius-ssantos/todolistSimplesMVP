package com.viniss.todo.resource

import com.viniss.todo.api.dto.CreateTaskRequest
import com.viniss.todo.api.dto.CreateTodoListRequest
import com.viniss.todo.api.dto.TaskResponse
import com.viniss.todo.api.dto.TodoListResponse
import com.viniss.todo.api.mapper.RequestMapper.toCommand
import com.viniss.todo.api.mapper.ResponseMapper.toResponse
import com.viniss.todo.service.port.CreateTaskUseCase
import com.viniss.todo.service.port.CreateTodoListUseCase
import com.viniss.todo.service.port.ListQueryUseCase
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/lists")
class TodoListController(
    private val listQueryUseCase: ListQueryUseCase,
    private val createTodoListUseCase: CreateTodoListUseCase,
    private val createTaskUseCase: CreateTaskUseCase
) {
    @GetMapping
    fun getAll(): List<TodoListResponse> =
        listQueryUseCase.findAllWithTasks().map { it.toResponse() }

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
}
