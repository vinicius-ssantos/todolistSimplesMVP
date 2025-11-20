package com.viniss.todo.service.port.write

import com.viniss.todo.service.model.TodoListView
import java.util.UUID

/**
 * Interface for updating TodoLists.
 *
 * Follows Interface Segregation Principle (ISP):
 * - Clients that only need to update lists depend on this minimal interface
 * - No dependency on create, delete, or task operations
 *
 * Used by: UpdateTodoListService
 */
interface TodoListUpdater {
    /**
     * Updates an existing TodoList.
     *
     * @param listId The ID of the list to update
     * @param name The new name for the list
     * @return The updated TodoList view
     */
    fun updateList(listId: UUID, name: String): TodoListView
}
