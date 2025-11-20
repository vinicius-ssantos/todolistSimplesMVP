package com.viniss.todo.repo

import com.viniss.todo.domain.TodoListEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

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

    // Query for pagination - IDs only (no join fetch to avoid pagination issues)
    @Query("""
        select list.id from TodoListEntity list
        where list.userId = :userId
    """)
    fun findIdsByUser(userId: UUID, pageable: Pageable): Page<UUID>

    // Fetch full entities with tasks for given IDs
    @Query("""
        select distinct list from TodoListEntity list
        left join fetch list.tasks tasks
        where list.id in :ids and list.userId = :userId
        order by list.createdAt asc, tasks.position asc
    """)
    fun findByIdsWithTasks(ids: List<UUID>, userId: UUID): List<TodoListEntity>

    @Query("""
        select list from TodoListEntity list
        left join fetch list.tasks
        where list.id = :id and list.userId = :userId
    """)
    fun findByIdWithTasksAndUser(id: UUID, userId: UUID): TodoListEntity?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from TodoListEntity list where list.id = :id and list.userId = :userId")
    fun deleteOwned(id: UUID, userId: UUID): Int
}
