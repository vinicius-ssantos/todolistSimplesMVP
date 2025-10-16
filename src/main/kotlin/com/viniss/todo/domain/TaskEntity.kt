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
    var position: Int = 0
) : BaseAudit() {

    @Column(name = "user_id", nullable = false)
    lateinit var userId: UUID

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as TaskEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
