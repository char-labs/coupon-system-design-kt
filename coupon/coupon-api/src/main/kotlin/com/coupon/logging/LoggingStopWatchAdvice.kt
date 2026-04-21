package com.coupon.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class LoggingStopWatchAdvice {
    companion object {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
        const val MAX_AFFORDABLE_TIME: Long = 3000
        private val HOT_ISSUE_REQUEST_URIS =
            setOf(
                "/coupon-issues",
                "/restaurant-coupons/issue",
            )
    }

    @Around("execution(* com.coupon.controller..*Controller.*(..))")
    fun stopWatchTarget(joinPoint: ProceedingJoinPoint): Any? {
        val startAt = System.currentTimeMillis()
        val proceed = joinPoint.proceed()
        val endAt = System.currentTimeMillis()
        val timeMs = (endAt - startAt)
        val request = currentRequestSnapshot()

        val className = joinPoint.signature.declaringType.simpleName
        val methodName = joinPoint.signature.name

        if (timeMs > MAX_AFFORDABLE_TIME) {
            log.warn(
                "method=${request?.method ?: "n/a"}, url=${request?.url ?: "n/a"}, call: $className - $methodName - timeMs:${timeMs}ms",
            )
            return proceed
        }

        if (shouldSkipInfoLog(request)) {
            return proceed
        }

        log.info(
            "method=${request?.method ?: "n/a"}, url=${request?.url ?: "n/a"}, call: $className - $methodName - timeMs:${timeMs}ms",
        )
        return proceed
    }

    private fun shouldSkipInfoLog(request: RequestSnapshot?): Boolean =
        request?.method == "POST" &&
            request.path in HOT_ISSUE_REQUEST_URIS

    private fun currentRequestSnapshot(): RequestSnapshot? {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request ?: return null

        val url =
            buildString(128) {
                append(request.requestURL)
                if (request.queryString != null) {
                    append("?")
                    append(request.queryString)
                }
            }

        return RequestSnapshot(
            method = request.method,
            path = request.requestURI,
            url = url,
        )
    }

    private data class RequestSnapshot(
        val method: String,
        val path: String,
        val url: String,
    )
}
