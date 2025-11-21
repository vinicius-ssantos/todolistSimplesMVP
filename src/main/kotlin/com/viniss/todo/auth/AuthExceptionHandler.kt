package com.viniss.todo.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Garante respostas JSON 401 para exceções propagadas fora do filtro.
 */
@RestControllerAdvice
class AuthExceptionHandler {

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(
        request: HttpServletRequest,
        ex: InvalidTokenException
    ): ResponseEntity<AuthErrorResponse> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(
                AuthErrorResponse(
                    error = "invalid_token",
                    message = ex.message,
                    path = request.requestURI ?: ""
                )
            )

    @ExceptionHandler(AccountLockedException::class)
    fun handleAccountLocked(
        request: HttpServletRequest,
        ex: AccountLockedException
    ): ResponseEntity<Map<String, String>> =
        ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(mapOf("error" to ex.message.orEmpty()))
}
