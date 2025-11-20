package com.viniss.todo.service.port.write

import com.viniss.todo.service.model.TodoListView

/**
 * Interface for creating TodoLists.
 *
 * Follows Interface Segregation Principle (ISP):
 * - Clients that only need to create lists depend on this minimal interface
 * - No dependency on update, delete, or task operations
 *
 * Used by: CreateTodoListService
 */
interface TodoListCreator {
    /**
     * Creates a new TodoList with the given name.
     *
     * @param name The name of the list
     * @return The created TodoList view
     */
    fun createList(name: String): TodoListView
}
