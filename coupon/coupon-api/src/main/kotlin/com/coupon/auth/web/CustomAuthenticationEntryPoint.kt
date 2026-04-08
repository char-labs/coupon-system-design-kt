package com.coupon.auth.web

import com.coupon.common.ApiResponse
import com.coupon.common.ErrorResponse
import com.coupon.enums.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authenticationException: AuthenticationException,
    ) {
        with(response) {
            status = HttpStatus.UNAUTHORIZED.value()
            contentType = MediaType.APPLICATION_JSON_VALUE
            characterEncoding = "UTF-8"
            writer.write(
                objectMapper.writeValueAsString(
                    ApiResponse.fail(
                        status = HttpStatus.UNAUTHORIZED.value(),
                        errorResponse =
                            ErrorResponse.of(
                                errorClassName = ErrorType.UNAUTHORIZED_TOKEN.name,
                                message = ErrorType.UNAUTHORIZED_TOKEN.message,
                            ),
                    ),
                ),
            )
        }
    }
}
