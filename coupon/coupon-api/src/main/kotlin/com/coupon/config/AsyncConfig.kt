package com.coupon.config

import com.coupon.async.LoggingAsyncExceptionHandler
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig(
    @Qualifier("applicationTaskExecutor")
    private val applicationTaskExecutor: AsyncTaskExecutor,
    private val loggingAsyncExceptionHandler: LoggingAsyncExceptionHandler,
) : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor = applicationTaskExecutor

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler = loggingAsyncExceptionHandler
}
