// src/main/kotlin/com/viniss/todo/domain/TaskEntity.kt
package com.viniss.todo.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate
import java.util.UUID
import org.hibernate.Hibernate

@Entity
@Table(
    name = "task",
    indexes = [Index(name = "idx_task_list_position", columnList = "list_id, position")]
)
class TaskEntity(
    @Id val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "list_id", nullable = false)
    var list: TodoListEntity,

    @field:NotBlank
    @Column(nullable = false, length = 140)
    var title: String,

    @Column(length = 1000)
    var notes: String? = null,

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    var priority: Priority = Priority.MEDIUM,

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    var status: Status = Status.OPEN,

    var dueDate: LocalDate? = null,

    @Column(nullable = false)
    var position: Int = 0,

    // Recurring task support
    @Column(name = "is_recurring", nullable = false)
    var isRecurring: Boolean = false,

    @Column(name = "recurrence_pattern", columnDefinition = "TEXT")
    var recurrencePatternJson: String? = null,

    @Column(name = "parent_recurring_task_id")
    var parentRecurringTaskId: UUID? = null // Link to the parent recurring task
) : BaseAudit() {

    @Column(name = "user_id", nullable = false)
    lateinit var userId: UUID

    // Many-to-many relationship with tags
    @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    @JoinTable(
        name = "task_tag",
        joinColumns = [JoinColumn(name = "task_id")],
        inverseJoinColumns = [JoinColumn(name = "tag_id")]
    )
    var tags: MutableSet<TagEntity> = mutableSetOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as TaskEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
