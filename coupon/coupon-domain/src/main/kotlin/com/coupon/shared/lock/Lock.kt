package com.coupon.shared.lock

import com.coupon.enums.error.ErrorType
import com.coupon.shared.tx.RequiresNewTransactionExecutor
import org.springframework.stereotype.Component

@Component
class Lock(
    private val lockExecutor: LockExecutor,
) {
    fun <T> withLock(
        key: String,
        timeoutMillis: Long = 5000,
        timeoutException: ErrorType = ErrorType.LOCK_ACQUISITION_FAILED,
        function: () -> T,
    ): T = lockExecutor.executeWithLock(key, timeoutMillis, timeoutException, function)

    fun <T> withLockRequiresNew(
        key: String,
        timeoutMillis: Long = 5000,
        timeoutException: ErrorType = ErrorType.LOCK_ACQUISITION_FAILED,
        function: () -> T,
    ): T = lockExecutor.executeWithLockRequiresNew(key, timeoutMillis, timeoutException, function)

    @Component
    class LockExecutor(
        private val lockRepository: LockRepository,
        private val requiresNewTransactionExecutor: RequiresNewTransactionExecutor,
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
                requiresNewTransactionExecutor.run(function)
            }
    }
}
