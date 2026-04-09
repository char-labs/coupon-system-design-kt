package com.coupon.coupon

import com.coupon.enums.coupon.CouponIssueResult
import java.time.Duration

/**
 * 쿠폰 발급 요청의 선행 상태를 Redis에서 관리하는 저장소다.
 *
 * 이 저장소의 목적은 최종 발급 row를 저장하는 것이 아니라, DB 저장 전에
 * "이미 누가 자리를 선점했는지", "동일 사용자가 중복 요청했는지", "남은 수량을 초과했는지"를
 * 빠르게 판별해 동시성 경쟁을 줄이는 데 있다.
 *
 * `reserve()`의 성공은 Redis 선점과 브로커 적재 전 단계가 통과됐다는 의미이며,
 * 최종 발급 완료는 worker가 DB 저장까지 마쳐야 확정된다.
 */
interface CouponIssueRedisRepository {
    /**
     * 발급 가능 수량 안에서 사용자 한 명의 발급 슬롯을 선점한다.
     * 중복 요청, 품절 여부를 Redis에서 먼저 판별해 즉시 결과를 반환한다.
     */
    fun reserve(
        couponId: Long,
        userId: Long,
        totalQuantity: Long,
        ttl: Duration,
    ): CouponIssueResult

    /**
     * 예약만 성공하고 후속 처리에 실패한 경우, 해당 사용자의 선점 상태를 되돌린다.
     */
    fun release(
        couponId: Long,
        userId: Long,
    )

    /**
     * 사용자 식별 정보는 유지한 채 점유 수량만 1 감소시킨다.
     * worker 보상 처리처럼 재고 슬롯만 반납해야 할 때 사용한다.
     */
    fun releaseStockSlot(couponId: Long)

    /**
     * Redis 상태가 비어 있을 때 DB 기준으로 점유 수량과 사용자 집합을 다시 적재한다.
     * 런타임 재시작이나 만료 이후 초기 복구 경로에서 사용된다.
     */
    fun rebuild(
        couponId: Long,
        occupiedCount: Long,
        userIds: Set<Long>,
        ttl: Duration,
    )

    /**
     * 해당 쿠폰의 발급 조율 상태가 Redis에 이미 올라와 있는지 확인한다.
     */
    fun hasState(couponId: Long): Boolean

    /**
     * 쿠폰 발급 상태를 강제로 비운다.
     * 테스트나 명시적 초기화처럼 상태를 완전히 버려야 할 때 사용한다.
     */
    fun clear(couponId: Long)
}
