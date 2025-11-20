package com.viniss.todo.service.list

import com.viniss.todo.service.port.DeleteTodoListUseCase
import com.viniss.todo.service.port.write.TodoListDeleter
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service responsible for deleting TodoLists.
 *
 * Follows SOLID principles:
 * - SRP: Only handles TodoList deletion
 * - ISP: Depends only on TodoListDeleter (1 method) instead of TodoListWriteRepository (6 methods)
 *
 * Benefits:
 * - Clearer dependencies (only deleteList() is needed)
 * - Easier to test (mock only TodoListDeleter)
 * - Better encapsulation (no access to unneeded create/update methods)
 */
@Service
@Primary
class DeleteTodoListService(
    private val todoListDeleter: TodoListDeleter
) : DeleteTodoListUseCase {

    @Transactional
    override fun delete(listId: UUID) {
        todoListDeleter.deleteList(listId)
    }
}
