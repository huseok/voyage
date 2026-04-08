package com.trioForce.voyage.common

import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BizException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBizException(ex: BizException): ApiResponse<Nothing> = ApiResponse(code = 400, message = ex.message ?: "bad request")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationException(ex: MethodArgumentNotValidException): ApiResponse<Nothing> {
        val firstError = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "validation failed"
        return ApiResponse(code = 400, message = firstError)
    }
}
