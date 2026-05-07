package jamgaVOCA.demo.api

import jamgaVOCA.demo.api.dto.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(e: IllegalArgumentException): ApiResponse<Nothing> =
        ApiResponse.error("BAD_REQUEST", e.message ?: "잘못된 요청입니다.")

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: NoSuchElementException): ApiResponse<Nothing> =
        ApiResponse.error("NOT_FOUND", e.message ?: "리소스를 찾을 수 없습니다.")

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneral(e: Exception): ApiResponse<Nothing> =
        ApiResponse.error("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.")
}
