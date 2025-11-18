package com.viniss.todo.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.util.*

/**
 * Entity representing a tag/category for organizing tasks.
 */
@Entity
@Table(
    name = "tag",
    indexes = [
        Index(name = "idx_tag_user_id", columnList = "user_id"),
        Index(name = "idx_tag_user_name", columnList = "user_id, name", unique = true)
    ]
)
class TagEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @field:NotBlank
    @Column(nullable = false, length = 50)
    var name: String,

    @Column(length = 7)
    var color: String? = null, // Hex color code (e.g., "#FF5733")

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    var tasks: MutableSet<TaskEntity> = mutableSetOf()
) : BaseAudit() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(other)) return false
        other as TagEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
