package com.coupon.enums.coupon

enum class CouponPreviewInapplicableReason(
    val description: String,
) {
    ALREADY_ISSUED("이미 발급받은 쿠폰입니다."),
    NOT_ACTIVE("활성화되지 않은 쿠폰입니다."),
    OUT_OF_STOCK("쿠폰 수량이 소진되었습니다."),
    EXPIRED("쿠폰 사용 가능 기간이 아닙니다."),
    BELOW_MIN_ORDER_AMOUNT("최소 주문 금액을 만족하지 않습니다."),
}
