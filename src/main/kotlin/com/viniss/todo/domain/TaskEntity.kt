// src/main/kotlin/com/viniss/todo/domain/TaskEntity.kt
package com.viniss.todo.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate
import java.util.*

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

    @Column(columnDefinition = "text")
    var notes: String? = null,

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    var priority: Priority = Priority.MEDIUM,

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    var status: Status = Status.OPEN,

    var dueDate: LocalDate? = null,

    @Column(nullable = false)
    var position: Int = 0
) : BaseAudit()
