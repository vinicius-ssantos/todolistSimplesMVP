package com.viniss.todo.auth


import org.springframework.data.jpa.repository.JpaRepository
import java.util.*


interface AppUserRepository : JpaRepository<AppUserEntity, UUID> {
    fun findByEmail(email: String): AppUserEntity?
    fun existsByEmail(email: String): Boolean
    fun findByEmailVerificationToken(token: String): AppUserEntity?
}