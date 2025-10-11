package com.viniss.todo.service

import com.viniss.todo.api.dto.TodoListResponse
import com.viniss.todo.api.mapper.toResponse
import com.viniss.todo.repo.TaskRepository
import com.viniss.todo.repo.TodoListRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListQueryService(
    private val listRepo: TodoListRepository,
    private val taskRepo: TaskRepository
) {
    @Transactional(readOnly = true)
    fun findAllWithTasks(): List<TodoListResponse> {
        val lists = listRepo.findAll(Sort.by(Sort.Direction.ASC, "createdAt"))
        return lists.map { list ->
            val tasks = taskRepo
                .findByListIdOrderByPositionAsc(
                    list.id)
                .map {
                    it.toResponse()
                }
            list.toResponse(tasks)
        }
    }
}