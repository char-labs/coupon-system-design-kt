package com.coupon.config

import com.coupon.user.User
import com.coupon.user.UserService
import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class UserArgumentResolver(
    private val userService: UserService,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean = parameter.parameterType == User::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        nativeWebRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): User {
        val authentication = SecurityContextHolder.getContext().authentication

        // 인증되지 않은 경우는 SecurityFilter에서 이미 차단됨
        // 여기서는 인증된 사용자만 오므로 User를 반환
        if (authentication == null || authentication.principal == "anonymousUser") {
            throw IllegalStateException("User is not authenticated")
        }

        return userService.getUser(authentication.principal as String)
    }
}
