package com.viniss.todo.service.domain

import com.viniss.todo.domain.TaskEntity
import com.viniss.todo.domain.TodoListEntity
import com.viniss.todo.repo.TaskRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Interface para gerenciamento de posições de tarefas.
 * Responsabilidade: manter a ordenação consistente de tarefas dentro de uma lista.
 */
interface TaskPositionManager {
    /**
     * Reorganiza as posições das tarefas quando uma tarefa é movida.
     *
     * @param list Lista que contém as tarefas
     * @param movedTask Tarefa sendo movida
     * @param newPosition Nova posição desejada
     */
    fun reorganizePositions(list: TodoListEntity, movedTask: TaskEntity, newPosition: Int)

    /**
     * Reorganiza as posições das tarefas sequencialmente (0, 1, 2, ...).
     *
     * @param listId ID da lista
     * @return Lista de tarefas reorganizadas
     */
    fun normalizePositions(listId: UUID): List<TaskEntity>
}

/**
 * Implementação padrão do gerenciador de posições de tarefas.
 * Mantém a invariante: posições são únicas e sequenciais dentro de uma lista.
 */
@Service
class DefaultTaskPositionManager(
    private val taskRepository: TaskRepository
) : TaskPositionManager {

    override fun reorganizePositions(list: TodoListEntity, movedTask: TaskEntity, newPosition: Int) {
        val allTasks = taskRepository.findByListIdOrderByPositionAsc(list.id)
        val oldPosition = movedTask.position

        // Não precisa reorganizar se a posição não mudou
        if (oldPosition == newPosition) {
            return
        }

        // Valida a nova posição
        require(newPosition >= 0) { "Position cannot be negative: $newPosition" }

        val maxPosition = allTasks.size - 1
        val targetPosition = minOf(newPosition, maxPosition)

        // Reorganiza as tarefas afetadas
        allTasks.forEach { task ->
            when {
                task.id == movedTask.id -> {
                    // A tarefa movida receberá a nova posição pelo caller
                }
                oldPosition < targetPosition -> {
                    // Movendo tarefa para baixo: desloca tarefas entre old e new para cima
                    if (task.position > oldPosition && task.position <= targetPosition) {
                        task.position--
                    }
                }
                oldPosition > targetPosition -> {
                    // Movendo tarefa para cima: desloca tarefas entre new e old para baixo
                    if (task.position >= targetPosition && task.position < oldPosition) {
                        task.position++
                    }
                }
            }
        }

        // Salva todas as tarefas afetadas (exceto a movida, que será salva pelo caller)
        val affectedTasks = allTasks.filter { it.id != movedTask.id }
        if (affectedTasks.isNotEmpty()) {
            taskRepository.saveAll(affectedTasks)
        }
    }

    override fun normalizePositions(listId: UUID): List<TaskEntity> {
        val tasks = taskRepository.findByListIdOrderByPositionAsc(listId)

        // Reorganiza sequencialmente
        tasks.forEachIndexed { index, task ->
            task.position = index
        }

        return if (tasks.isNotEmpty()) {
            taskRepository.saveAll(tasks).toList()
        } else {
            emptyList()
        }
    }
}
