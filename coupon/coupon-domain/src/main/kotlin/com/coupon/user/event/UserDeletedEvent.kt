package com.coupon.user.event

/** 사용자 삭제 이벤트
 */
data class UserDeletedEvent(
    val userId: Long,
    val userKey: String,
    val email: String,
)
