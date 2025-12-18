package jamgaVOCA.demo.generateSkill.dto

data class SkillGenerateResponse(
    val id: Long?,
    val word: String,

    val name: String,
    val description: String,
    val damage: Int,
    val imageDesc: String,

    /**
     * 프론트 표시용 Base64 PNG (data:image/png;base64, 를 붙여서 사용)
     * 이미지 생성 실패 시 null 가능
     */
    val imageBase64: String?,

    /**
     * 이미지 생성 상태(디버깅/보고서용). 프론트는 선택적으로 표시 가능.
     */
    val imageStatus: ImageStatus,

    /**
     * 실패 시 서버가 전달하는 에러/사유(선택).
     * - 예: "content_policy_violation", "invalid_request_error", "401 Unauthorized" 등
     * 성공이면 null 권장
     */
    val imageError: String? = null
)

enum class ImageStatus {
    SUCCESS,
    FAILED
}
