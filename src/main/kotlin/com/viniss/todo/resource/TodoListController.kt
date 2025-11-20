package com.viniss.todo.resource

import com.viniss.todo.api.dto.*
import com.viniss.todo.api.mapper.RequestMapper.toCommand
import com.viniss.todo.api.mapper.ResponseMapper.toResponse
import com.viniss.todo.service.port.*
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
    private val updateTodoListUseCase: UpdateTodoListUseCase,
    private val deleteTodoListUseCase: DeleteTodoListUseCase
) {
    @GetMapping
    fun getAll(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "name") sortBy: String,
        @RequestParam(defaultValue = "ASC") sortDirection: String
    ): Page<TodoListResponse> {
        val direction = if (sortDirection.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))
        return listQueryUseCase.findAllWithTasks(pageable).map { it.toResponse() }
    }

    @GetMapping("/{listId}")
    fun getById(@PathVariable listId: UUID): TodoListResponse =
        listQueryUseCase.findById(listId).toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createList(@Valid @RequestBody request: CreateTodoListRequest): TodoListResponse =
        createTodoListUseCase.create(request.toCommand()).toResponse()

    @PatchMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateList(@PathVariable listId: UUID, @Valid @RequestBody body: UpdateTodoListRequest): ResponseEntity<Void> {
        updateTodoListUseCase.update(listId, body.toCommand())
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteList(@PathVariable listId: UUID): ResponseEntity<Void> {
        deleteTodoListUseCase.delete(listId)
        return ResponseEntity.noContent().build()
    }

}
