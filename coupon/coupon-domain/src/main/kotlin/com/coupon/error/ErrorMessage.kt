package com.coupon.error

import com.coupon.enums.error.ErrorType

data class ErrorMessage(
    val code: String,
    val message: String,
) {
    constructor(
        errorType: ErrorType,
        errorData: Any? = null,
    ) : this(
        code = errorType.name,
        message = (errorData?.toString() ?: errorType.message),
    )
}
