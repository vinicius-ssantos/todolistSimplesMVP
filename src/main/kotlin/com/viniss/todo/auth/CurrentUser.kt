package com.viniss.todo.auth

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CurrentUser {
    fun id(): UUID {
        val auth = SecurityContextHolder.getContext().authentication
            ?: error("Usuário não autenticado")
        val p = auth.principal
        return when (p) {
            is AuthUser -> p.id
            is String   -> UUID.fromString(p) // fallback se algum teste setar name=UUID
            is SpringUser -> UUID.fromString(p.username)  //
            else        -> error("Principal não contém userId")
        }
    }
}