package com.coupon.support.lock

import com.coupon.enums.ErrorType
import org.springframework.stereotype.Component

@Component
class Lock(_lockAdvice: LockAdvice) {

    init {
        lockAdvice = _lockAdvice
    }

    companion object {
        private lateinit var lockAdvice: LockAdvice

        fun <T> executeWithLock(
            key: String,
            timeoutMillis: Long = 5000,
            timeoutException: ErrorType = ErrorType.LOCK_ACQUISITION_FAILED,
            function: () -> T,
        ): T {
            return lockAdvice.executeWithLock(key, timeoutMillis, timeoutException, function)
        }
    }

    @Component
    class LockAdvice(
        private val lockRepository: LockRepository,
    ) {
        fun <T> executeWithLock(
            key: String,
            timeoutMillis: Long = 5000,
            timeoutException: ErrorType = ErrorType.LOCK_ACQUISITION_FAILED,
            function: () -> T,
        ): T {
            return lockRepository.executeWithLock(key, timeoutMillis, timeoutException, function)
        }
    }
}
