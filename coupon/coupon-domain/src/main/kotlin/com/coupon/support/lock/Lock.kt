package com.coupon.support.lock

import com.coupon.enums.ErrorType
import com.coupon.support.tx.Tx
import org.springframework.stereotype.Component

@Component
class Lock(
    _lockAdvice: LockAdvice,
) {
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
        ): T = lockAdvice.executeWithLock(key, timeoutMillis, timeoutException, function)

        /**
         * Use this for mutable critical sections that must commit before the lock is released.
         * A new transaction prevents the lock-protected work from leaking into an outer transaction.
         */
        fun <T> executeWithLockRequiresNew(
            key: String,
            timeoutMillis: Long = 5000,
            timeoutException: ErrorType = ErrorType.LOCK_ACQUISITION_FAILED,
            function: () -> T,
        ): T = lockAdvice.executeWithLockRequiresNew(key, timeoutMillis, timeoutException, function)
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
        ): T = lockRepository.executeWithLock(key, timeoutMillis, timeoutException, function)

        fun <T> executeWithLockRequiresNew(
            key: String,
            timeoutMillis: Long = 5000,
            timeoutException: ErrorType = ErrorType.LOCK_ACQUISITION_FAILED,
            function: () -> T,
        ): T =
            lockRepository.executeWithLock(key, timeoutMillis, timeoutException) {
                Tx.requiresNew(function)
            }
    }
}
