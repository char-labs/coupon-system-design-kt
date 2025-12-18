package com.coupon.config

import com.coupon.enums.ErrorLevel
import com.coupon.error.ErrorException
import com.coupon.support.logging.logger
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import java.lang.reflect.Method

class AsyncExceptionHandler : AsyncUncaughtExceptionHandler {
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
