package com.viniss.todo.resource

import com.viniss.todo.api.dto.TodoListResponse
import com.viniss.todo.api.mapper.ResponseMapper.toResponse
import com.viniss.todo.service.port.ListQueryUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/lists")
class TodoListController(
    private val listQueryUseCase: ListQueryUseCase
) {
    @GetMapping
    fun getAll(): List<TodoListResponse> =
        listQueryUseCase.findAllWithTasks().map { it.toResponse() }
}
