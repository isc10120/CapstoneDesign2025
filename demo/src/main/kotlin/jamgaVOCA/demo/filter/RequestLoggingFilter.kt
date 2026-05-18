package jamgaVOCA.demo.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RequestLoggingFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val start = System.currentTimeMillis()
        log.info("[REQUEST] ${request.method} ${request.requestURI} from ${request.remoteAddr}")
        filterChain.doFilter(request, response)
        val elapsed = System.currentTimeMillis() - start
        log.info("[RESPONSE] ${response.status} ${request.method} ${request.requestURI} (${elapsed}ms)")
    }
}