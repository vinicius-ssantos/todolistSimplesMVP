package com.viniss.todo.repo

import com.viniss.todo.domain.TodoListEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface TodoListRepository : JpaRepository<TodoListEntity, UUID> {
    @Query(
        """
            select distinct list from TodoListEntity list
            left join fetch list.tasks tasks
            order by list.createdAt asc, tasks.position asc
        """
    )
    fun findAllWithTasksOrdered(): List<TodoListEntity>

    @Query(
        """
            select list from TodoListEntity list
            left join fetch list.tasks
            where list.id = :id
        """
    )
    fun findByIdWithTasks(id: UUID): TodoListEntity?

    @Query("""
        select distinct list from TodoListEntity list
        left join fetch list.tasks tasks
        where list.userId = :userId
        order by list.createdAt asc, tasks.position asc
    """)
    fun findAllWithTasksOrderedByUser(userId: UUID): List<TodoListEntity>

    @Query("""
        select list from TodoListEntity list
        left join fetch list.tasks
        where list.id = :id and list.userId = :userId
    """)
    fun findByIdWithTasksAndUser(id: UUID, userId: UUID): TodoListEntity?

    fun existsByIdAndUserId(id: UUID, userId: UUID): Boolean
}
