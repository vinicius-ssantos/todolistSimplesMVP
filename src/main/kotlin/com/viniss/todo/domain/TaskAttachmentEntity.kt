package com.viniss.todo.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.util.*

/**
 * Entity representing a file attachment for a task.
 */
@Entity
@Table(
    name = "task_attachment",
    indexes = [
        Index(name = "idx_attachment_task_id", columnList = "task_id"),
        Index(name = "idx_attachment_user_id", columnList = "user_id")
    ]
)
class TaskAttachmentEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "task_id", nullable = false)
    var taskId: UUID,

    @field:NotBlank
    @Column(nullable = false, length = 255)
    var fileName: String,

    @Column(nullable = false, length = 100)
    var contentType: String,

    @Column(nullable = false)
    var fileSize: Long, // Size in bytes

    @field:NotBlank
    @Column(nullable = false, length = 500)
    var storageUrl: String, // URL or path to the stored file

    @Column(name = "user_id", nullable = false)
    var userId: UUID

) : BaseAudit() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(other)) return false
        other as TaskAttachmentEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
