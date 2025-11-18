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
    ): ResponseEntity<ValidationErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { error ->
            FieldValidationError(
                field = error.field,
                message = error.defaultMessage ?: "Erro de validação",
                rejectedValue = error.rejectedValue
            )
        }

        val response = ValidationErrorResponse(
            code = "VALIDATION_ERROR",
            message = "Erros de validação encontrados",
            errors = errors
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response)
    }
}

/**
 * Response padronizado para erros de validação.
 */
data class ValidationErrorResponse(
    val code: String,
    val message: String,
    val errors: List<FieldValidationError>
)

/**
 * Detalhes de um erro de validação em um campo específico.
 */
data class FieldValidationError(
    val field: String,
    val message: String,
    val rejectedValue: Any?
)
