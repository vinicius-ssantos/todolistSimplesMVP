package com.viniss.todo.service.port.write

import java.util.UUID

/**
 * Interface for deleting TodoLists.
 *
 * Follows Interface Segregation Principle (ISP):
 * - Clients that only need to delete lists depend on this minimal interface
 * - No dependency on create, update, or task operations
 *
 * Used by: DeleteTodoListService
 */
interface TodoListDeleter {
    /**
     * Deletes a TodoList by ID.
     *
     * @param listId The ID of the list to delete
     */
    fun deleteList(listId: UUID)
}
