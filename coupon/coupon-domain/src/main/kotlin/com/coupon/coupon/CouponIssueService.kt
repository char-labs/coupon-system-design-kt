package com.coupon.coupon

import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.coupon.event.CouponLifecycleDomainEvent
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.lock.Lock
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import com.coupon.support.tx.Tx
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime

@Service
class CouponIssueService(
    private val couponIssueRepository: CouponIssueRepository,
    private val couponIssueValidator: CouponIssueValidator,
    private val couponIssueRedisRepository: CouponIssueRedisRepository,
    private val couponIssueEventPublisher: CouponIssueEventPublisher,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    fun reserveIssue(
        coupon: CouponDetail,
        userId: Long,
    ): CouponIssueResult {
        ensureInitialized(coupon)
        return couponIssueRedisRepository.reserve(
            couponId = coupon.id,
            userId = userId,
            totalQuantity = coupon.totalQuantity,
            ttl = stateTtl(coupon.endAt),
        )
    }

    fun publishIssue(
        couponId: Long,
        userId: Long,
    ) {
        couponIssueEventPublisher.publish(
            CouponIssueMessage(
                couponId = couponId,
                userId = userId,
            ),
        )
    }

    /**
     * Shared issuance core used after coupon validation and stock decrement are already handled outside.
     * Duplicate issuance is still delegated to the DB unique constraint so the transaction rolls back cleanly.
     */
    fun executeIssue(command: CouponIssueCommand.Issue): CouponIssue =
        Tx.writeable {
            val couponIssue = couponIssueRepository.save(CouponIssueCriteria.Create.of(command))

            applicationEventPublisher.publishEvent(
                CouponLifecycleDomainEvent.Issued(
                    couponIssueId = couponIssue.id,
                    couponId = couponIssue.couponId,
                    userId = couponIssue.userId,
                    occurredAt = LocalDateTime.now(),
                ),
            )

            couponIssue
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

    fun rebuildState(coupon: CouponDetail) {
        val issuedUsers = couponIssueRepository.findUserIdsByCouponId(coupon.id)
        val occupiedCount = coupon.totalQuantity - coupon.remainingQuantity
        couponIssueRedisRepository.rebuild(
            couponId = coupon.id,
            occupiedCount = occupiedCount,
            userIds = issuedUsers,
            ttl = stateTtl(coupon.endAt),
        )
    }

    /**
     * Coupon use is still a synchronous source-of-truth update.
     * The outbox event only represents a follow-up projection, not the state transition itself.
     */
    fun useCoupon(command: CouponIssueCommand.Use): CouponIssue.Detail =
        Lock.executeWithLockRequiresNew(
            key = "COUPON_ISSUE_STATUS:${command.couponIssueId}",
        ) {
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
                    occurredAt = LocalDateTime.now(),
                ),
            )

            couponIssueRepository.findDetailById(command.couponIssueId)
        }

    fun cancelIssue(command: CouponIssueCommand.Cancel): CouponIssue {
        val couponIssue = couponIssueRepository.findById(command.couponIssueId)
        couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId)
        val canceled = couponIssueRepository.cancelIfIssued(command.couponIssueId)
        if (!canceled) {
            throw ErrorException(com.coupon.enums.error.ErrorType.INVALID_COUPON_STATUS)
        }

        applicationEventPublisher.publishEvent(
            CouponLifecycleDomainEvent.Canceled(
                couponIssueId = couponIssue.id,
                couponId = couponIssue.couponId,
                userId = couponIssue.userId,
                occurredAt = LocalDateTime.now(),
            ),
        )

        return couponIssue
    }

    private fun ensureInitialized(coupon: CouponDetail) {
        if (couponIssueRedisRepository.hasState(coupon.id)) {
            return
        }

        Lock.executeWithLock(
            key = "COUPON_ISSUE_STATE_INIT:${coupon.id}",
            timeoutMillis = STATE_INIT_LOCK_TIMEOUT_MILLIS,
        ) {
            if (couponIssueRedisRepository.hasState(coupon.id)) {
                return@executeWithLock
            }

            rebuildState(coupon)
        }
    }

    private fun stateTtl(endAt: LocalDateTime): Duration {
        val now = LocalDateTime.now()
        val expiresAt = endAt.plusDays(1)
        return when {
            expiresAt.isAfter(now) -> Duration.between(now, expiresAt)
            else -> MINIMUM_STATE_TTL
        }
    }

    companion object {
        private const val STATE_INIT_LOCK_TIMEOUT_MILLIS = 1_000L
        private val MINIMUM_STATE_TTL: Duration = Duration.ofMinutes(10)
    }
}
