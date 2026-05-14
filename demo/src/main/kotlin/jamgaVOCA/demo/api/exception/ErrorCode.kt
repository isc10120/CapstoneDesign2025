package jamgaVOCA.demo.api.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    // 인증
    // UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_EXPIRED", "액세스 토큰이 만료되었습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증되지 않은 사용자입니다."),
    INVALID_AUTH_INFO(HttpStatus.UNAUTHORIZED, "INVALID_AUTH_INFO", "인증 정보가 올바르지 않습니다."),

    // 유저
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 유저입니다."),
    USER_SETTINGS_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_SETTINGS_NOT_FOUND", "유저 설정을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),
    DUMMY_USER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "DUMMY_USER_NOT_FOUND", "더미 유저가 존재하지 않습니다."),

    // 배틀
    BATTLE_NOT_FOUND(HttpStatus.NOT_FOUND, "BATTLE_NOT_FOUND", "진행 중인 배틀이 없습니다."),

    // 스킬
    SKILL_NOT_FOUND(HttpStatus.NOT_FOUND, "SKILL_NOT_FOUND", "존재하지 않는 스킬입니다."),

    // 단어
    WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "WORD_NOT_FOUND", "존재하지 않는 단어입니다."),
    NOT_COLLECTED_THIS_WEEK(HttpStatus.BAD_REQUEST, "NOT_COLLECTED_THIS_WEEK", "이번 주에 수집된 단어가 아닙니다."),
    INVALID_WORD_LEVEL(HttpStatus.BAD_REQUEST, "INVALID_WORD_LEVEL", "유효하지 않은 단어 난이도입니다."),
    DAILY_NUDGE_WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "DAILY_NUDGE_WORD_NOT_FOUND", "일일 넛지 단어를 찾을 수 없습니다."),

    // 문제
    WORD_TOO_SHORT_FOR_SPELLING(HttpStatus.BAD_REQUEST, "WORD_TOO_SHORT_FOR_SPELLING", "3글자 이하 단어는 spelling 문제를 지원하지 않습니다."),
    NOUN_NOT_SUPPORTED_FOR_SYNONYM(HttpStatus.BAD_REQUEST, "NOUN_NOT_SUPPORTED_FOR_SYNONYM", "명사는 유의어 문제를 지원하지 않습니다."),
    UNSUPPORTED_QUESTION_TYPE(HttpStatus.BAD_REQUEST, "UNSUPPORTED_QUESTION_TYPE", "지원하지 않는 문제 유형입니다."),

    // AI
    AI_CHAT_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_CHAT_CALL_FAILED", "AI 채팅 호출에 실패했습니다."),
    AI_JSON_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_JSON_PARSE_FAILED", "AI 응답 JSON 파싱에 실패했습니다."),
    AI_IMAGE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_IMAGE_GENERATION_FAILED", "AI 이미지 생성에 실패했습니다."),
    AI_IMAGE_RESPONSE_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "AI_IMAGE_RESPONSE_INVALID", "AI 이미지 응답이 올바르지 않습니다."),

    // 공통
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.")
}