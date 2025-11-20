package com.viniss.todo.service.port

import com.viniss.todo.service.port.write.*

/**
 * Composite interface for all write operations on TodoLists and Tasks.
 *
 * Refactored to follow Interface Segregation Principle (ISP):
 * - This interface aggregates all write operations for backward compatibility
 * - Individual clients should depend on specific segregated interfaces instead
 * - Each segregated interface has a single, focused responsibility
 *
 * Segregated interfaces:
 * - TodoListCreator: Only list creation
 * - TodoListUpdater: Only list updates
 * - TodoListDeleter: Only list deletion
 * - TaskCreator: Only task creation
 * - TaskUpdater: Only task updates
 * - TaskDeleter: Only task deletion
 *
 * Benefits:
 * - Clients depend only on methods they actually use (ISP compliance)
 * - Better testability (mock only needed interface)
 * - Clearer dependencies and intent
 * - Easier to maintain and extend
 */
interface TodoListWriteRepository :
    TodoListCreator,
    TodoListUpdater,
    TodoListDeleter,
    TaskCreator,
    TaskUpdater,
    TaskDeleter {
    // All methods inherited from segregated interfaces
    // No additional methods needed here
}
