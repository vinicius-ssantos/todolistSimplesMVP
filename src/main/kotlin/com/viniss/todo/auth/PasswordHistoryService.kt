package com.viniss.todo.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Configuration properties for password history.
 */
@ConfigurationProperties("app.password.history")
data class PasswordHistoryProps(
    val enabled: Boolean = true,
    val preventReuseCount: Int = 5
)

/**
 * Service for managing password history and preventing password reuse.
 */
@Service
@EnableConfigurationProperties(PasswordHistoryProps::class)
@Transactional
class PasswordHistoryService(
    private val repository: PasswordHistoryRepository,
    private val encoder: PasswordEncoder,
    private val props: PasswordHistoryProps
) {

    /**
     * Checks if a password has been used recently by the user.
     *
     * @param userId The user ID
     * @param rawPassword The password to check
     * @return true if the password has been used recently, false otherwise
     */
    fun isPasswordReused(userId: UUID, rawPassword: String): Boolean {
        if (!props.enabled) {
            return false
        }

        val pageable = PageRequest.of(0, props.preventReuseCount)
        val recentPasswords = repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)

        return recentPasswords.any { history ->
            encoder.matches(rawPassword, history.passwordHash)
        }
    }

    /**
     * Records a new password in the user's password history.
     *
     * @param userId The user ID
     * @param passwordHash The BCrypt hash of the new password
     */
    fun recordPasswordChange(userId: UUID, passwordHash: String) {
        if (!props.enabled) {
            return
        }

        // Save the new password hash to history
        repository.save(
            PasswordHistoryEntity(
                userId = userId,
                passwordHash = passwordHash
            )
        )

        // Clean up old entries beyond the retention limit
        // Keep preventReuseCount + current password (so +1)
        cleanupOldEntries(userId, props.preventReuseCount + 1)
    }

    /**
     * Deletes old password history entries beyond the retention limit.
     *
     * @param userId The user ID
     * @param keepCount Number of most recent entries to keep
     */
    private fun cleanupOldEntries(userId: UUID, keepCount: Int) {
        val pageable = PageRequest.of(0, Int.MAX_VALUE)
        val allEntries = repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
        if (allEntries.size > keepCount) {
            val entriesToDelete = allEntries.drop(keepCount)
            repository.deleteAll(entriesToDelete)
        }
    }

    /**
     * Gets the number of recent passwords being tracked for reuse prevention.
     */
    fun getPreventReuseCount(): Int = props.preventReuseCount

    /**
     * Checks if password history is enabled.
     */
    fun isEnabled(): Boolean = props.enabled
}
