package com.coupon.controller.advice

import com.coupon.common.ApiResponse
import com.coupon.common.ErrorResponse
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.shared.logging.logger
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice(basePackages = ["com.coupon"])
class ApiExceptionAdvice : ResponseEntityExceptionHandler() {
    companion object {
        private val log by logger()
    }

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errorResponse = ErrorResponse.of(ex.javaClass.simpleName, ex.message ?: "No message available")
        val apiResponse = ApiResponse.fail(statusCode.value(), errorResponse)
        return super.handleExceptionInternal(ex, apiResponse, headers, statusCode, request)
    }

    override fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.error(e) { "MethodArgumentNotValidException: ${e.message}" }
        val errors = e.bindingResult.allErrors.mapNotNull { it.defaultMessage }
        val errorMessage = if (errors.isNotEmpty()) errors.joinToString("; ") else "Validation failed"
        val errorResponse = ErrorResponse.of(e.javaClass.simpleName, errorMessage)
        val apiResponse = ApiResponse.fail(status.value(), errorResponse)
        return ResponseEntity.status(status).body(apiResponse)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(e: ConstraintViolationException): ResponseEntity<ApiResponse<ErrorResponse>> {
        log.error(e) { "ConstraintViolationException: ${e.message}" }
        val bindingErrors =
            e.constraintViolations.associate { violation ->
                val path = violation.propertyPath.toString().substringAfterLast(".", "unknown")
                path to violation.message
            }
        val errorResponse = ErrorResponse.of(e.javaClass.simpleName, bindingErrors.toString())
        val apiResponse = ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), errorResponse)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    protected fun handleMethodArgumentTypeMismatchException(
        e: MethodArgumentTypeMismatchException,
    ): ResponseEntity<ApiResponse<ErrorResponse>> {
        log.error(e) { "MethodArgumentTypeMismatchException: ${e.message}" }
        val errorCode: ErrorType = ErrorType.METHOD_ARGUMENT_TYPE_MISMATCH
        val errorResponse = ErrorResponse.of(e.javaClass.simpleName, errorCode.message)
        val apiResponse = ApiResponse.fail(errorCode.status, errorResponse)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse)
    }

    override fun handleHttpRequestMethodNotSupported(
        e: HttpRequestMethodNotSupportedException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        log.error(e) { "HttpRequestMethodNotSupportedException: ${e.message}" }
        val errorCode: ErrorType = ErrorType.METHOD_NOT_ALLOWED
        val errorResponse = ErrorResponse.of(e.javaClass.simpleName, errorCode.message)
        val apiResponse = ApiResponse.fail(errorCode.status, errorResponse)
        return ResponseEntity.status(errorCode.status).body(apiResponse)
    }

    @ExceptionHandler(ErrorException::class)
    fun handleCustomException(e: ErrorException): ResponseEntity<ApiResponse<ErrorResponse>> {
        log.error(e) { "CustomException: ${e.message}" }
        val errorCode: ErrorType = e.errorType
        val errorResponse = ErrorResponse.of(errorCode.name, errorCode.message)
        val apiResponse = ApiResponse.fail(errorCode.status, errorResponse)
        return ResponseEntity
            .status(errorCode.status)
            .apply {
                if (errorCode == ErrorType.LOCK_ACQUISITION_FAILED) {
                    header(HttpHeaders.RETRY_AFTER, "1")
                }
            }.body(apiResponse)
    }

    @ExceptionHandler(Exception::class)
    protected fun handleException(e: Exception): ResponseEntity<ApiResponse<ErrorResponse>> {
        log.error(e) { "Internal Server Error: ${e.message}" }
        val internalServerError: ErrorType = ErrorType.INTERNAL_SERVER_ERROR
        val errorResponse = ErrorResponse.of(e.javaClass.simpleName, internalServerError.message)
        val apiResponse = ApiResponse.fail(internalServerError.status, errorResponse)
        return ResponseEntity.status(internalServerError.status).body(apiResponse)
    }
}
