package com.viniss.todo.auth

/**
 * Exception thrown when an account is temporarily locked due to too many failed login attempts.
 * This exception should result in HTTP 429 (Too Many Requests) response.
 */
class AccountLockedException(message: String) : RuntimeException(message)
