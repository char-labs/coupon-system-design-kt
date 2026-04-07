package com.coupon.enums.coupon

enum class CouponIssueResult(
    val description: String,
) {
    SUCCESS("쿠폰 발급 요청이 성공적으로 접수되었습니다. 잠시 후 쿠폰함에서 확인해주세요."),
    DUPLICATE("아쉽지만, 쿠폰이 모두 소진되었습니다."),
    SOLD_OUT("이미 참여하셨습니다."),
}
