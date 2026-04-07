package com.coupon.enums.coupon

enum class CouponTrafficPolicy(
    val description: String,
) {
    /**
     * 레거시 정책.
     * 공개 API는 더 이상 이 값을 생성하지 않고, 기존 데이터 정규화를 위해서만 남겨둔다.
     */
    NORMAL_SYNC(
        description = "레거시 동기 발급 정책. 신규 쿠폰에는 사용하지 않는다.",
    ),

    /**
     * 공개 발급의 표준 정책.
     * `POST /coupon-issues` Redis 선점 + Kafka 비동기 실행 경로를 사용한다.
     */
    HOT_FCFS_ASYNC(
        description = "비동기 발급 정책. `POST /coupon-issues` 경로와 strict FCFS를 사용한다.",
    ),
}
