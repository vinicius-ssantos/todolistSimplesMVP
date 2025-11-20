package com.viniss.todo.service.port.write

import java.util.UUID

/**
 * Interface for deleting Tasks within TodoLists.
 *
 * Follows Interface Segregation Principle (ISP):
 * - Clients that only need to delete tasks depend on this minimal interface
 * - No dependency on list operations or task create/update
 *
 * Used by: DeleteTaskService
 */
interface TaskDeleter {
    /**
     * Deletes a task from a TodoList.
     *
     * @param listId The ID of the list containing the task
     * @param taskId The ID of the task to delete
     */
    fun deleteTask(listId: UUID, taskId: UUID)
}
