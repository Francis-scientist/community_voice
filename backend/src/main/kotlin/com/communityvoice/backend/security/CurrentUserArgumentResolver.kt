package com.communityvoice.backend.security

import com.communityvoice.backend.service.UnauthorizedException
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * Teaches Spring how to fill in an `@CurrentUser user: AuthUser` parameter.
 *
 * AuthTokenFilter has already done the work of reading the token; all this has
 * to do is hand over what the filter found - or refuse the request with 401 if
 * the endpoint declared that it needs somebody signed in.
 *
 * Registered in config/WebConfig.kt.
 */
@Component
class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(CurrentUser::class.java) &&
            AuthUser::class.java.isAssignableFrom(parameter.parameterType)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        val user = webRequest.getAttribute(
            AuthTokenFilter.ATTRIBUTE,
            NativeWebRequest.SCOPE_REQUEST
        ) as? AuthUser

        val annotation = parameter.getParameterAnnotation(CurrentUser::class.java)
        if (user == null && annotation?.required != false) {
            throw UnauthorizedException()
        }
        return user
    }
}
