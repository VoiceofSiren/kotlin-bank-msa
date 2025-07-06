package com.example.bank.common

import com.fasterxml.jackson.annotation.JsonInclude
import com.sun.net.httpserver.Authenticator
import org.springframework.http.ResponseEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val err: Error? = null
) {
    companion object {
        fun <T> success(data: T, message: String): ResponseEntity<ApiResponse<T>> {
            return ResponseEntity.ok(ApiResponse(true, message, data))
        }

        fun <T> error(
            message: String,
            errCode: String? = null,
            details: Any? = null,
            path: String? = null
        ): ResponseEntity<ApiResponse<T>> {
            return ResponseEntity.badRequest().body(
                ApiResponse(false, message, null, Error(errCode, details, path))
            )
        }

        fun <T> exceptionError(
            message: String,
            errCode: String? = null,
            details: Any? = null,
            path: String? = null
        ): ApiResponse<T> {
            return ApiResponse(false, message, null, Error(errCode, details, path))
        }
    }
}

data class Error (
    val code: String? = null,
    val details: Any? = null,
    val path: String? = null
)