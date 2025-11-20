package com.viniss.todo.service.port.write

import com.viniss.todo.service.model.TaskCreationData
import com.viniss.todo.service.model.TaskView
import java.util.UUID

/**
 * Interface for creating Tasks within TodoLists.
 *
 * Follows Interface Segregation Principle (ISP):
 * - Clients that only need to create tasks depend on this minimal interface
 * - No dependency on list operations or task update/delete
 *
 * Used by: CreateTaskService
 */
interface TaskCreator {
    /**
     * Adds a new task to a TodoList.
     *
     * @param listId The ID of the list to add the task to
     * @param task The task data for creation
     * @return The created Task view
     */
    fun addTask(listId: UUID, task: TaskCreationData): TaskView
}
