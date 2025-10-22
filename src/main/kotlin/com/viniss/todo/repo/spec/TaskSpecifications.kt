package com.viniss.todo.repo.spec

import com.viniss.todo.api.dto.DueFilter
import com.viniss.todo.domain.Priority
import com.viniss.todo.domain.Status
import com.viniss.todo.domain.TaskEntity
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate
import java.util.UUID

object TaskSpecifications {

    fun byOwner(userId: UUID) = Specification<TaskEntity> { root, _, cb ->
        cb.equal(root.get<UUID>("userId"), userId)
    }

    fun byList(listId: UUID) = Specification<TaskEntity> { root, _, cb ->
        cb.equal(root.get<UUID>("list").get<UUID>("id"), listId)
    }

    fun byStatuses(statuses: Set<Status>?) = Specification<TaskEntity> { root, _, _ ->
        if (statuses.isNullOrEmpty()) null else root.get<Status>("status").`in`(statuses)
    }

    fun byPriority(priority: Priority?) = Specification<TaskEntity> { root, _, cb ->
        if (priority == null) null else cb.equal(root.get<Priority>("priority"), priority)
    }

    fun byDue(due: DueFilter) = Specification<TaskEntity> { root, _, cb ->
        when (due) {
            DueFilter.ALL -> null
            DueFilter.TODAY -> cb.equal(root.get<LocalDate>("dueDate"), LocalDate.now())
            DueFilter.WEEK -> cb.between(root.get("dueDate"), LocalDate.now(), LocalDate.now().plusDays(7))
            DueFilter.OVERDUE -> cb.lessThan(root.get("dueDate"), LocalDate.now())
        }
    }

    fun search(q: String?) = Specification<TaskEntity> { root, _, cb ->
        if (q.isNullOrBlank()) null
        else {
            val like = "%${q.lowercase()}%"
            cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("notes")), like)
            )
        }
    }


    fun allOf(listId: UUID, userId: UUID, due: DueFilter, statuses: Set<Status>?, q: String?, priority: Priority?) =
        Specification.where(byOwner(userId))
            .and(byList(listId))
            .and(byDue(due))
            .and(byStatuses(statuses))
            .and(search(q))
            .and(byPriority(priority))
}
