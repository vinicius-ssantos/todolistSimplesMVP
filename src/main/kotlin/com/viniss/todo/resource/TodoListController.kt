package com.viniss.todo.resource

import com.viniss.todo.api.dto.TodoListResponse
import com.viniss.todo.service.ListQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/lists")
class TodoListController(
    private val listQueryService: ListQueryService
) {
    @GetMapping
    fun getAll(): List<TodoListResponse> = listQueryService.findAllWithTasks()
}
