package jamgaVOCA.demo.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminKeyInterceptor(
    @Value("\${admin.secret}") private val secret: String
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val key = request.getHeader("X-Admin-Key")
        if (key != secret) {
            response.status = 403
            response.contentType = "application/json;charset=UTF-8"
            response.writer.write("""{"success":false,"error":"FORBIDDEN","message":"어드민 키가 올바르지 않습니다."}""")
            return false
        }
        return true
    }
}
