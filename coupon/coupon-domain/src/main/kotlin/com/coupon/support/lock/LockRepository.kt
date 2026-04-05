package com.coupon.support.lock

import com.coupon.enums.error.ErrorType

interface LockRepository {
    fun tryLock(
        key: String,
        timeoutMillis: Long,
    ): Boolean

    fun unlock(key: String)

    fun <T> executeWithLock(
        key: String,
        timeoutMillis: Long = 5000,
        timeoutException: ErrorType = ErrorType.LOCK_ACQUISITION_FAILED,
        func: () -> T,
    ): T
}
