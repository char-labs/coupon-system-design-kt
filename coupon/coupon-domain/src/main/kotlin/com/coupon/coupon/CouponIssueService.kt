package com.coupon.coupon

import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.coupon.event.CouponLifecycleDomainEvent
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.shared.page.OffsetPageRequest
import com.coupon.shared.page.Page
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.Thread.sleep
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class CouponIssueService(
    private val couponIssueRepository: CouponIssueRepository,
    private val couponIssueValidator: CouponIssueValidator,
    private val couponIssueRedisRepository: CouponIssueRedisRepository,
    private val couponIssueStateInitializationExecutor: CouponIssueStateInitializationExecutor,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
) {
    fun reserveIssue(
        coupon: CouponDetail,
        userId: Long,
    ): CouponIssueResult {
        val ttl = stateTtl(coupon.endAt)

        return try {
            // 정상 burst 경로에서는 Lua 한 번으로 중복/재고를 판정한다.
            couponIssueRedisRepository.reserve(
                couponId = coupon.id,
                userId = userId,
                totalQuantity = coupon.totalQuantity,
                ttl = ttl,
            )
        } catch (e: CouponIssueStateNotInitializedException) {
            initializeIssueStateOrWait(coupon, ttl)
            couponIssueRedisRepository.reserve(
                couponId = coupon.id,
                userId = userId,
                totalQuantity = coupon.totalQuantity,
                ttl = ttl,
            )
        }
    }

    /**
     * 실제 발급 row 저장은 여기서만 처리한다.
     * 쿠폰 유효성 검증과 재고 차감은 바깥 단계에서 끝났다고 가정한다.
     * 중복 발급은 DB unique constraint가 마지막 방어선 역할을 한다.
     */
    @Transactional
    fun executeIssue(command: CouponIssueCommand.Issue): CouponIssue =
        couponIssueRepository.save(CouponIssueCriteria.Create.of(command)).also { couponIssue ->
            applicationEventPublisher.publishEvent(
                CouponLifecycleDomainEvent.Issued(
                    couponIssueId = couponIssue.id,
                    couponId = couponIssue.couponId,
                    userId = couponIssue.userId,
                    occurredAt = LocalDateTime.ofInstant(clock.instant(), clock.zone),
                ),
            )
        }

    fun getCouponIssue(couponIssueId: Long): CouponIssue.Detail = couponIssueRepository.findDetailById(couponIssueId)

    fun getMyCoupons(
        userId: Long,
        request: OffsetPageRequest,
    ): Page<CouponIssue.Detail> = couponIssueRepository.findAllByUserId(userId, request)

    fun getCouponIssues(
        couponId: Long,
        request: OffsetPageRequest,
    ): Page<CouponIssue.Detail> = couponIssueRepository.findAllByCouponId(couponId, request)

    fun release(
        couponId: Long,
        userId: Long,
    ) {
        couponIssueRedisRepository.release(couponId, userId)
    }

    fun releaseStockSlot(couponId: Long) {
        couponIssueRedisRepository.releaseStockSlot(couponId)
    }

    fun diagnoseIssueState(coupon: CouponDetail): CouponIssueStateDiagnostic {
        val issuedCount = couponIssueRepository.countByCouponId(coupon.id)
        return CouponIssueStateDiagnostic(
            couponId = coupon.id,
            initialized = couponIssueRedisRepository.hasState(coupon.id),
            occupiedCount = coupon.totalQuantity - coupon.remainingQuantity,
            issuedCount = issuedCount,
        )
    }

    fun rebuildIssueState(coupon: CouponDetail): CouponIssueStateDiagnostic {
        couponIssueStateInitializationExecutor.rebuildState(coupon, stateTtl(coupon.endAt))
        return diagnoseIssueState(coupon)
    }

    /**
     * 쿠폰 사용은 지금도 동기식 원본 상태 변경이다.
     * outbox 이벤트는 후속 projection을 위한 것이지, 상태 전이 자체를 대신하지 않는다.
     */
    fun useCoupon(command: CouponIssueCommand.Use): CouponIssue.Detail {
        val couponIssue = couponIssueRepository.findById(command.couponIssueId)
        couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId)

        val used = couponIssueRepository.useIfIssued(command.couponIssueId)
        if (!used) {
            throw ErrorException(ErrorType.INVALID_COUPON_STATUS)
        }

        applicationEventPublisher.publishEvent(
            CouponLifecycleDomainEvent.Used(
                couponIssueId = couponIssue.id,
                couponId = couponIssue.couponId,
                userId = couponIssue.userId,
                occurredAt = LocalDateTime.ofInstant(clock.instant(), clock.zone),
            ),
        )

        return couponIssueRepository.findDetailById(command.couponIssueId)
    }

    @Transactional
    fun cancelIssue(command: CouponIssueCommand.Cancel): CouponIssue {
        val couponIssue = couponIssueRepository.findById(command.couponIssueId)
        couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId)
        val canceled = couponIssueRepository.cancelIfIssued(command.couponIssueId)
        if (!canceled) {
            throw ErrorException(ErrorType.INVALID_COUPON_STATUS)
        }

        applicationEventPublisher.publishEvent(
            CouponLifecycleDomainEvent.Canceled(
                couponIssueId = couponIssue.id,
                couponId = couponIssue.couponId,
                userId = couponIssue.userId,
                occurredAt = LocalDateTime.ofInstant(clock.instant(), clock.zone),
            ),
        )

        return couponIssue
    }

    /**
     * 다른 요청이 Redis 상태를 복구 중이면
     * 뒤따르는 요청은 짧게 기다렸다가 복구된 상태를 재사용한다.
     */
    private fun waitForInitializedState(couponId: Long) {
        val deadline = System.nanoTime() + Duration.ofMillis(STATE_INIT_WAIT_TIMEOUT_MILLIS).toNanos()

        while (System.nanoTime() < deadline) {
            if (couponIssueRedisRepository.hasState(couponId)) {
                return
            }

            sleep(STATE_INIT_POLL_INTERVAL_MILLIS)
        }

        throw ErrorException(ErrorType.LOCK_ACQUISITION_FAILED)
    }

    private fun initializeIssueStateOrWait(
        coupon: CouponDetail,
        ttl: Duration,
    ) {
        try {
            couponIssueStateInitializationExecutor.initializeStateIfAbsent(coupon, ttl)
        } catch (e: ErrorException) {
            if (e.errorType != ErrorType.LOCK_ACQUISITION_FAILED) {
                throw e
            }

            waitForInitializedState(coupon.id)
        }
    }

    private fun stateTtl(endAt: LocalDateTime): Duration {
        val now = LocalDateTime.ofInstant(clock.instant(), clock.zone)
        val expiresAt = endAt.plusDays(1)
        return when {
            expiresAt.isAfter(now) -> Duration.between(now, expiresAt)
            else -> MINIMUM_STATE_TTL
        }
    }

    companion object {
        private const val STATE_INIT_WAIT_TIMEOUT_MILLIS = 2_000L
        private const val STATE_INIT_POLL_INTERVAL_MILLIS = 25L
        private val MINIMUM_STATE_TTL: Duration = Duration.ofMinutes(10)
    }
}
