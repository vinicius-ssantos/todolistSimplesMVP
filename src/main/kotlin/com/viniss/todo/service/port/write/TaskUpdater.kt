package com.viniss.todo.service.port.write

import com.viniss.todo.service.model.TaskView
import java.util.UUID

/**
 * Interface for updating Tasks within TodoLists.
 *
 * Follows Interface Segregation Principle (ISP):
 * - Clients that only need to update tasks depend on this minimal interface
 * - No dependency on list operations or task create/delete
 *
 * Used by: UpdateTaskService
 */
interface TaskUpdater {
    /**
     * Updates a task within a TodoList.
     *
     * @param listId The ID of the list containing the task
     * @param taskId The ID of the task to update
     * @param updates Map of field names to new values
     * @return The updated Task view
     */
    fun updateTask(listId: UUID, taskId: UUID, updates: Map<String, Any?>): TaskView
}
