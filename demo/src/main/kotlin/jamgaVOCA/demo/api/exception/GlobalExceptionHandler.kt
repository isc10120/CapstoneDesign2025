package jamgaVOCA.demo.api.exception

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jamgaVOCA.demo.api.dto.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AppException::class)
    fun handleAppException(e: AppException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.errorCode.code, e.message))

//    @ExceptionHandler(ExpiredJwtException::class)
//    @ResponseStatus(HttpStatus.UNAUTHORIZED)
//    fun handleExpiredJwt(e: ExpiredJwtException): ApiResponse<Nothing> =
//        ApiResponse.error(ErrorCode.ACCESS_TOKEN_EXPIRED.code, ErrorCode.ACCESS_TOKEN_EXPIRED.message)
//
//    @ExceptionHandler(JwtException::class)
//    @ResponseStatus(HttpStatus.UNAUTHORIZED)
//    fun handleJwt(e: JwtException): ApiResponse<Nothing> =
//        ApiResponse.error(ErrorCode.INVALID_TOKEN.code, ErrorCode.INVALID_TOKEN.message)

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(e: IllegalArgumentException): ApiResponse<Nothing> =
        ApiResponse.error(ErrorCode.BAD_REQUEST.code, e.message ?: ErrorCode.BAD_REQUEST.message)

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGeneral(e: Exception): ApiResponse<Nothing> =
        ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.code, ErrorCode.INTERNAL_SERVER_ERROR.message)
}