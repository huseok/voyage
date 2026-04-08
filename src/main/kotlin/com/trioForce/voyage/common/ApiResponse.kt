package com.trioForce.voyage.common

data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "ok",
    val data: T? = null
)

fun <T> ok(data: T): ApiResponse<T> = ApiResponse(data = data)
