package jamgaVOCA.demo.api.exception

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jamgaVOCA.demo.api.dto.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    // 커스텀 앱 예외
    @ExceptionHandler(AppException::class)
    fun handleAppException(e: AppException): ResponseEntity<ApiResponse<Nothing>> {
        log.warn("[EXCEPTION] AppException - code=${e.errorCode.code}, message=${e.message}")
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.errorCode.code, e.message))
    }

//    // JWT 만료
//    @ExceptionHandler(ExpiredJwtException::class)
//    fun handleExpiredJwt(e: ExpiredJwtException): ResponseEntity<ApiResponse<Nothing>> =
//        ResponseEntity
//            .status(HttpStatus.UNAUTHORIZED)
//            .body(ApiResponse.error(ErrorCode.ACCESS_TOKEN_EXPIRED.code, ErrorCode.ACCESS_TOKEN_EXPIRED.message))
//
//    // JWT 유효하지 않음
//    @ExceptionHandler(JwtException::class)
//    fun handleJwt(e: JwtException): ResponseEntity<ApiResponse<Nothing>> =
//        ResponseEntity
//            .status(HttpStatus.UNAUTHORIZED)
//            .body(ApiResponse.error(ErrorCode.INVALID_TOKEN.code, ErrorCode.INVALID_TOKEN.message))
//
//    // 권한 없음
//    @ExceptionHandler(AccessDeniedException::class)
//    fun handleAccessDenied(e: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> =
//        ResponseEntity
//            .status(HttpStatus.FORBIDDEN)
//            .body(ApiResponse.error(ErrorCode.FORBIDDEN.code, ErrorCode.FORBIDDEN.message))

    // 파라미터 타입 불일치 (쿼리/경로 파라미터 잘못 전달)
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorCode.BAD_REQUEST.code, "'${e.name}' 파라미터의 형식이 올바르지 않습니다."))

    // 필수 쿼리 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(e: MissingServletRequestParameterException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorCode.BAD_REQUEST.code, "'${e.parameterName}' 파라미터가 필요합니다."))

    // @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorCode.BAD_REQUEST.code, message))
    }

    // JSON body 파싱 실패 (잘못된 형식)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(e: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorCode.BAD_REQUEST.code, "요청 본문의 형식이 올바르지 않습니다."))

    // 존재하지 않는 경로
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNotFound(e: NoHandlerFoundException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ErrorCode.NOT_FOUND.code, ErrorCode.NOT_FOUND.message))

    // 지원하지 않는 HTTP 메서드
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED.code, ErrorCode.METHOD_NOT_ALLOWED.message))

    // IllegalArgumentException
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorCode.BAD_REQUEST.code, e.message ?: ErrorCode.BAD_REQUEST.message))

    // 나머지 모든 예외
    @ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        log.error("[EXCEPTION] 처리되지 않은 예외 - ${e.javaClass.simpleName}: ${e.message}", e)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.code, ErrorCode.INTERNAL_SERVER_ERROR.message))
    }
}