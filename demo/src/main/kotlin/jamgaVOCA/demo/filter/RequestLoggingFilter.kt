package jamgaVOCA.demo.filter

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jamgaVOCA.demo.domain.user.User
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.lang.Exception

@Component
class RequestLoggingFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val start = System.currentTimeMillis()
        val cachingRequest = ContentCachingRequestWrapper(request)
        val cachingResponse = ContentCachingResponseWrapper(response)

        // 응답 인코딩을 강제로 UTF-8로 설정하여 로깅 시 한글 깨짐 방지
        cachingResponse.characterEncoding = "UTF-8"

        filterChain.doFilter(cachingRequest, cachingResponse)

        val elapsed = System.currentTimeMillis() - start
        val userId = getUserId()
        val rawBody = getBody(cachingResponse.contentAsByteArray, cachingResponse.characterEncoding)
        val formattedBody = formatJson(rawBody)

        log.info("[USER: $userId] [REQUEST] ${request.method} ${request.requestURI} from ${request.remoteAddr}")
        log.info("[USER: $userId] [RESPONSE] ${response.status} ${request.method} ${request.requestURI} (${elapsed}ms) Body:\n$formattedBody")

        cachingResponse.copyBodyToResponse()
    }

    private fun getUserId(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication != null && authentication.principal is User) {
            (authentication.principal as User).id?.toString() ?: "UNKNOWN"
        } else {
            "ANONYMOUS"
        }
    }

    private fun getBody(content: ByteArray, contentEncoding: String?): String {
        if (content.isEmpty()) return "[empty]"
        return try {
            // contentEncoding이 null이거나 비어있으면 UTF-8을 기본으로 사용
            val charset = if (contentEncoding.isNullOrBlank()) {
                Charsets.UTF_8
            } else {
                try {
                    charset(contentEncoding)
                } catch (e: Exception) {
                    Charsets.UTF_8
                }
            }
            String(content, charset)
        } catch (e: Exception) {
            "[unable to decode body]"
        }
    }

    private fun formatJson(json: String): String {
        if (json == "[empty]" || json == "[unable to decode body]") return json
        return try {
            val jsonObject = objectMapper.readValue(json, Any::class.java)
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject)
        } catch (e: Exception) {
            json // JSON이 아니면 그냥 반환
        }
    }
}