package com.viniss.todo.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Configuration properties for password validation rules.
 */
@ConfigurationProperties("app.password")
data class PasswordProps(
    val minLength: Int = 12,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigit: Boolean = true,
    val requireSpecialChar: Boolean = true,
    val specialChars: String = "!@#\$%^&*()_+-=[]{}|;:,.<>?"
)

/**
 * Password validation result.
 */
sealed class PasswordValidationResult {
    object Valid : PasswordValidationResult()
    data class Invalid(val errors: List<String>) : PasswordValidationResult()

    fun isValid(): Boolean = this is Valid
    fun getErrors(): List<String> = when (this) {
        is Invalid -> errors
        is Valid -> emptyList()
    }
}

/**
 * Service for validating password strength and complexity.
 */
@Component
@EnableConfigurationProperties(PasswordProps::class)
class PasswordValidator(
    private val props: PasswordProps
) {

    /**
     * Validates a password against configured complexity requirements.
     *
     * @param password The password to validate
     * @return PasswordValidationResult indicating if valid or list of errors
     */
    fun validate(password: String): PasswordValidationResult {
        val errors = mutableListOf<String>()

        // Check minimum length
        if (password.length < props.minLength) {
            errors.add("Password must be at least ${props.minLength} characters long")
        }

        // Check for uppercase letter
        if (props.requireUppercase && !password.any { it.isUpperCase() }) {
            errors.add("Password must contain at least one uppercase letter")
        }

        // Check for lowercase letter
        if (props.requireLowercase && !password.any { it.isLowerCase() }) {
            errors.add("Password must contain at least one lowercase letter")
        }

        // Check for digit
        if (props.requireDigit && !password.any { it.isDigit() }) {
            errors.add("Password must contain at least one digit")
        }

        // Check for special character
        if (props.requireSpecialChar && !password.any { it in props.specialChars }) {
            errors.add("Password must contain at least one special character (${props.specialChars})")
        }

        // Check for common weak patterns
        val weakPatterns = listOf(
            "password", "12345", "qwerty", "abc123", "letmein",
            "welcome", "monkey", "dragon", "master", "sunshine"
        )
        if (weakPatterns.any { password.lowercase().contains(it) }) {
            errors.add("Password contains common weak patterns")
        }

        // Check for sequential characters (e.g., "123", "abc")
        if (hasSequentialChars(password)) {
            errors.add("Password contains sequential characters")
        }

        return if (errors.isEmpty()) {
            PasswordValidationResult.Valid
        } else {
            PasswordValidationResult.Invalid(errors)
        }
    }

    /**
     * Checks if password contains 3+ sequential characters.
     */
    private fun hasSequentialChars(password: String): Boolean {
        for (i in 0 until password.length - 2) {
            val char1 = password[i].code
            val char2 = password[i + 1].code
            val char3 = password[i + 2].code

            // Check ascending (e.g., "abc", "123")
            if (char2 == char1 + 1 && char3 == char2 + 1) {
                return true
            }

            // Check descending (e.g., "cba", "321")
            if (char2 == char1 - 1 && char3 == char2 - 1) {
                return true
            }
        }
        return false
    }

    /**
     * Gets a human-readable description of password requirements.
     */
    fun getRequirementsDescription(): String {
        val requirements = mutableListOf<String>()
        requirements.add("At least ${props.minLength} characters")
        if (props.requireUppercase) requirements.add("One uppercase letter")
        if (props.requireLowercase) requirements.add("One lowercase letter")
        if (props.requireDigit) requirements.add("One digit")
        if (props.requireSpecialChar) requirements.add("One special character")
        return requirements.joinToString(", ")
    }
}
