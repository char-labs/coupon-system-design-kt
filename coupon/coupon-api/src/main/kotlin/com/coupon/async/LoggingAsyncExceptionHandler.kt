package com.coupon.async

import com.coupon.enums.error.ErrorLevel
import com.coupon.error.ErrorException
import com.coupon.shared.logging.logger
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.stereotype.Component
import java.lang.reflect.Method

@Component
class LoggingAsyncExceptionHandler : AsyncUncaughtExceptionHandler {
    private val logger by logger()

    override fun handleUncaughtException(
        e: Throwable,
        method: Method,
        vararg params: Any?,
    ) {
        if (e is ErrorException) {
            when (e.errorType.level) {
                ErrorLevel.ERROR -> logger.error { "${"ErrorException : {}"} ${e.message} $e" }
                ErrorLevel.WARN -> logger.warn { "${"ErrorException : {}"} ${e.message} $e" }
                else -> logger.info { "${"ErrorException : {}"} ${e.message} $e" }
            }
        } else {
            logger.error { "${"Exception : {}"} ${e.message} $e" }
        }
    }
}
