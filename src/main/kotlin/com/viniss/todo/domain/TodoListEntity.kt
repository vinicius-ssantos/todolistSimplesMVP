package com.viniss.todo.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import java.util.UUID
import org.hibernate.Hibernate

@Entity
@Table(name = "todo_list")
class TodoListEntity(
    @Id val id: UUID = UUID.randomUUID(),

    @field:NotBlank
    @Column(nullable = false, length = 100)
    var name: String
) : BaseAudit() {

    @Column(name = "user_id", nullable = false)
    lateinit var userId: UUID

    @OneToMany(
        mappedBy = "list",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var tasks: MutableList<TaskEntity> = mutableListOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as TodoListEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
