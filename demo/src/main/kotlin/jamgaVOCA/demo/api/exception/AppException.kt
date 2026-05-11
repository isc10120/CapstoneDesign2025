package jamgaVOCA.demo.api.exception

class AppException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message
) : RuntimeException(message) // 상위 레이어나 라이브러리로 전달될 때 안전하게 하기위해 부모도 초기화