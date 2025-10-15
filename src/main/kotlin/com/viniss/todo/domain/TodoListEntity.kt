package com.viniss.todo.domain

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.util.*

@Entity @Table(name = "todo_list")
class TodoListEntity(
    @Id val id: UUID = UUID.randomUUID(),

    @field:NotBlank
    @Column(nullable = false, length = 100)
    var name: String
) : BaseAudit() {

    @Column(name = "user_id", columnDefinition = "UUID")
    var userId: UUID? = null   // ficará NOT NULL depois (V5)

    @OneToMany(
        mappedBy = "list",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var tasks: MutableList<TaskEntity> = mutableListOf()
}
