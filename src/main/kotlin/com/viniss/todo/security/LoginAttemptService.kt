package com.viniss.todo.security

import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class LoginAttemptService {

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MINUTES = 15L
    }

    private val attemptsCache = ConcurrentHashMap<String, MutableList<LocalDateTime>>()
    private val lockoutCache = ConcurrentHashMap<String, LocalDateTime>()

    fun recordFailedAttempt(email: String) {
        val now = LocalDateTime.now()
        val attempts = attemptsCache.computeIfAbsent(email) { mutableListOf() }

        // Remove attempts older than 15 minutes
        attempts.removeIf { it.isBefore(now.minusMinutes(LOCKOUT_DURATION_MINUTES)) }

        attempts.add(now)

        // Lock account if max attempts exceeded
        if (attempts.size >= MAX_ATTEMPTS) {
            lockoutCache[email] = now
        }
    }

    fun resetFailedAttempts(email: String) {
        attemptsCache.remove(email)
        lockoutCache.remove(email)
    }

    fun isBlocked(email: String): Boolean {
        val lockoutTime = lockoutCache[email] ?: return false

        val now = LocalDateTime.now()
        val lockoutExpiry = lockoutTime.plusMinutes(LOCKOUT_DURATION_MINUTES)

        return if (now.isBefore(lockoutExpiry)) {
            true
        } else {
            // Lockout expired, remove from cache
            lockoutCache.remove(email)
            attemptsCache.remove(email)
            false
        }
    }

    fun getRemainingAttempts(email: String): Int {
        val now = LocalDateTime.now()
        val attempts = attemptsCache[email]?.filter {
            it.isAfter(now.minusMinutes(LOCKOUT_DURATION_MINUTES))
        } ?: emptyList()

        return (MAX_ATTEMPTS - attempts.size).coerceAtLeast(0)
    }
}
