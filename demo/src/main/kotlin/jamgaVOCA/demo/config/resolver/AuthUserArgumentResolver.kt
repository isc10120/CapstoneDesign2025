package jamgaVOCA.demo.config.resolver

import jamgaVOCA.demo.api.annotation.AuthUser
import jamgaVOCA.demo.api.exception.AppException
import jamgaVOCA.demo.api.exception.ErrorCode
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
            ?: throw AppException(ErrorCode.UNAUTHENTICATED)

        return authentication.principal as? User
            ?: throw AppException(ErrorCode.INVALID_AUTH_INFO)
    }
}
