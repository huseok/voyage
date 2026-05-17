package com.trioForce.voyage.common

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BizException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBizException(ex: BizException): ApiResponse<Nothing> = ApiResponse(code = 400, message = ex.message ?: "bad request")

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ApiResponse<Nothing> {
        val raw = ex.message?.lowercase() ?: ""
        val message = when {
            "code" in raw || "t_categories" in raw -> "分类编码已存在，请更换编码"
            else -> "数据冲突，请检查是否重复"
        }
        return ApiResponse(code = 400, message = message)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationException(ex: MethodArgumentNotValidException): ApiResponse<Nothing> {
        val firstError = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "validation failed"
        return ApiResponse(code = 400, message = firstError)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiResponse<Nothing>> {
        val status = ex.statusCode.value()
        return ResponseEntity
            .status(ex.statusCode)
            .body(ApiResponse(code = status, message = ex.reason ?: "error", data = null))
    }
}
