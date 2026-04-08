package jamgaVOCA.demo.api.dto

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError? = null
) {
    companion object {
        fun <T> success(data: T?): ApiResponse<T> = ApiResponse(true, data, null)
        fun <T> error(code: String, message: String): ApiResponse<T> = ApiResponse(false, null, ApiError(code, message))
    }
}

data class ApiError(
    val code: String,
    val message: String
)
