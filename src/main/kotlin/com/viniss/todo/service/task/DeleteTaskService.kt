package com.viniss.todo.service.task

import com.viniss.todo.service.port.DeleteTaskUseCase
import com.viniss.todo.service.port.write.TaskDeleter
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service responsible for deleting Tasks.
 *
 * Follows SOLID principles:
 * - SRP: Only handles Task deletion
 * - ISP: Depends only on TaskDeleter (1 method) instead of TodoListWriteRepository (6 methods)
 *
 * Benefits:
 * - Clearer dependencies (only deleteTask() is needed)
 * - Easier to test (mock only TaskDeleter)
 * - Better encapsulation (no access to unneeded list/create/update methods)
 */
@Service
@Primary
class DeleteTaskService(
    private val taskDeleter: TaskDeleter
) : DeleteTaskUseCase {

    @Transactional
    override fun delete(listId: UUID, taskId: UUID) {
        taskDeleter.deleteTask(listId, taskId)
    }
}
