package com.coupon.shared.lock

import com.coupon.enums.error.ErrorType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WithDistributedLock(
    val key: String,
    val timeoutMillis: Long = 5000,
    val timeoutException: ErrorType = ErrorType.LOCK_ACQUISITION_FAILED,
    val requiresNew: Boolean = false,
)
