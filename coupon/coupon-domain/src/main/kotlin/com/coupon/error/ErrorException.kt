package com.coupon.error

import com.coupon.enums.ErrorType

data class ErrorException(
    val errorType: ErrorType,
    val data: Any? = null,
) : RuntimeException(errorType.message)
