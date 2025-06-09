package com.natter.api.exception

import java.time.LocalDateTime
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
            ex: MethodArgumentNotValidException
    ): ResponseEntity<ValidationErrorResponse> {
        val errors =
                ex.bindingResult.fieldErrors.map { fieldError ->
                    ValidationError(
                            field = fieldError.field,
                            message = fieldError.defaultMessage ?: "Invalid value",
                            rejectedValue = fieldError.rejectedValue?.toString()
                    )
                }

        val errorResponse =
                ValidationErrorResponse(
                        timestamp = LocalDateTime.now(),
                        status = HttpStatus.BAD_REQUEST.value(),
                        error = "Validation Failed",
                        message = "Request validation failed",
                        errors = errors
                )

        return ResponseEntity.badRequest().body(errorResponse)
    }

    data class ValidationErrorResponse(
            val timestamp: LocalDateTime,
            val status: Int,
            val error: String,
            val message: String,
            val errors: List<ValidationError>
    )

    data class ValidationError(val field: String, val message: String, val rejectedValue: String?)

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred"
        )
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    data class ErrorResponse(
        val timestamp: LocalDateTime,
        val status: Int,
        val error: String,
        val message: String
    )
}
