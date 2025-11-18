package com.viniss.todo.resource

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Handler global para exceções de validação Jakarta Bean Validation.
 * Captura erros de @Valid e retorna respostas padronizadas.
 */
@RestControllerAdvice
class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<ApiErrorResponse> {
        // Return the first validation error message for simplicity
        val firstError = ex.bindingResult.fieldErrors.firstOrNull()
        val message = firstError?.defaultMessage ?: "Validation error"

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse(message = message))
    }
}
