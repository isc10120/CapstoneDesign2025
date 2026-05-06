package jamgaVOCA.demo.config.resolver

import jamgaVOCA.demo.api.annotation.AuthUser
import jamgaVOCA.demo.domain.user.User
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class AuthUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthUser::class.java) &&
                parameter.parameterType == User::class.java
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): User {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("인증되지 않은 사용자입니다.")

        return authentication.principal as? User
            ?: throw IllegalStateException("인증 정보가 올바르지 않습니다.")
    }
}
