package com.viniss.todo.service.port

import com.viniss.todo.service.model.TaskCreationData
import com.viniss.todo.service.model.TaskView
import com.viniss.todo.service.model.TodoListView
import java.util.UUID

interface TodoListWriteRepository {
    fun createList(name: String): TodoListView
    fun addTask(listId: UUID, task: TaskCreationData): TaskView
    fun updateList(listId: UUID, name: String): TodoListView
    fun updateTask(listId: UUID, taskId: UUID, updates: Map<String, Any?>): TaskView
    fun deleteList(listId: UUID)
    fun deleteTask(listId: UUID, taskId: UUID)
}
