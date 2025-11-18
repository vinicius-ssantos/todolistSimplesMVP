package com.viniss.todo.domain

import jakarta.persistence.*
import java.util.*

/**
 * Enum representing the permission level for shared lists.
 */
enum class SharePermission {
    READ,      // Can only view tasks
    WRITE,     // Can view and edit tasks
    ADMIN      // Can view, edit, and manage sharing
}

/**
 * Entity representing a shared todo list collaboration.
 */
@Entity
@Table(
    name = "shared_list",
    indexes = [
        Index(name = "idx_shared_list_list_id", columnList = "list_id"),
        Index(name = "idx_shared_list_shared_with_user_id", columnList = "shared_with_user_id"),
        Index(name = "idx_shared_list_unique", columnList = "list_id, shared_with_user_id", unique = true)
    ]
)
class SharedListEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "list_id", nullable = false)
    var listId: UUID,

    @Column(name = "shared_by_user_id", nullable = false)
    var sharedByUserId: UUID,

    @Column(name = "shared_with_user_id", nullable = false)
    var sharedWithUserId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var permission: SharePermission = SharePermission.READ

) : BaseAudit() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(other)) return false
        other as SharedListEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
