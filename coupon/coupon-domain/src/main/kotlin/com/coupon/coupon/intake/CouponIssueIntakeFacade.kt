package com.coupon.coupon.intake

import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.CouponService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.shared.logging.logger
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class CouponIssueIntakeFacade(
    private val couponService: CouponService,
    private val couponIssueService: CouponIssueService,
    private val couponIssueMessagePublisher: CouponIssueMessagePublisher,
    private val clock: Clock,
    private val metrics: CouponIssueFlowMetrics,
) {
    private val log by logger()

    /**
     * intake 경로는 "즉시 판정 가능한 단계"만 담당한다.
     * 1. 발급 가능한 쿠폰 조회 및 검증
     * 2. Redis에서 중복/재고 slot 선점
     * 3. Kafka에 accepted command 발행
     *
     * 여기서 `SUCCESS`는 reserve와 broker ack가 끝났다는 뜻이다.
     * 최종 DB 반영은 worker가 비동기로 처리한다.
     */
    fun issue(command: CouponIssueCommand.Issue): CouponIssueResult {
        val requestId = UUID.randomUUID().toString()
        val coupon = couponService.getAvailableCouponForIssue(command.couponId)
        val reserveStartedAt = System.nanoTime()
        val reserveResult =
            couponIssueService.reserveIssue(coupon, command.userId).also { reserveResult ->
                metrics.recordIntakeReserve(
                    result = reserveResult,
                    duration = Duration.ofNanos((System.nanoTime() - reserveStartedAt).coerceAtLeast(0)),
                )
                if (reserveResult == CouponIssueResult.SUCCESS) {
                    log.info {
                        "event=coupon.issue phase=intake.reserve result=${reserveResult.name} requestId=$requestId " +
                            "couponId=${command.couponId} userId=${command.userId} errorType=NONE"
                    }
                } else {
                    log.debug {
                        "event=coupon.issue phase=intake.reserve result=${reserveResult.name} requestId=$requestId " +
                            "couponId=${command.couponId} userId=${command.userId} errorType=NONE"
                    }
                }
            }

        if (reserveResult != CouponIssueResult.SUCCESS) {
            return reserveResult
        }

        val message =
            CouponIssueMessage(
                couponId = command.couponId,
                userId = command.userId,
                requestId = requestId,
                acceptedAt = Instant.now(clock),
            )

        val publishStartedAt = System.nanoTime()

        return try {
            val receipt = couponIssueMessagePublisher.publish(message)
            val publishDuration = Duration.ofNanos((System.nanoTime() - publishStartedAt).coerceAtLeast(0))
            metrics.recordIntakePublish(result = "SUCCESS", duration = publishDuration)

            log.info {
                "event=coupon.issue phase=intake.publish result=SUCCESS requestId=${message.requestId} " +
                    "couponId=${message.couponId} userId=${message.userId} acceptedAt=${message.acceptedAt} " +
                    "durationMs=${publishDuration.toMillis()} errorType=NONE " +
                    "topic=${receipt.topic} partition=${receipt.partition} offset=${receipt.offset}"
            }

            CouponIssueResult.SUCCESS
        } catch (exception: Exception) {
            // Redis reserve가 끝난 뒤 publish가 실패하면
            // 선점한 상태를 반드시 되돌려야 한다.
            val publishDuration = Duration.ofNanos((System.nanoTime() - publishStartedAt).coerceAtLeast(0))
            metrics.recordIntakePublish(result = "FAILURE", duration = publishDuration)

            releaseReservedSlotOrThrow(message)

            log.error(exception) {
                "event=coupon.issue phase=intake.publish result=FAILURE requestId=${message.requestId} " +
                    "couponId=${message.couponId} userId=${message.userId} acceptedAt=${message.acceptedAt} " +
                    "durationMs=${publishDuration.toMillis()} errorType=${ErrorType.COUPON_ISSUE_KAFKA_ERROR.name}"
            }

            throw ErrorException(ErrorType.COUPON_ISSUE_KAFKA_ERROR)
        }
    }

    private fun releaseReservedSlotOrThrow(message: CouponIssueMessage) {
        runCatching {
            couponIssueService.release(message.couponId, message.userId)
        }.onSuccess {
            metrics.recordIntakeCompensation(result = "SUCCESS")
        }.onFailure { compensationError ->
            metrics.recordIntakeCompensation(result = "FAILURE")
            log.error(compensationError) {
                "event=coupon.issue phase=intake.compensation result=FAILURE requestId=${message.requestId} " +
                    "couponId=${message.couponId} userId=${message.userId} " +
                    "errorType=${ErrorType.COUPON_ISSUE_REDIS_ERROR.name}"
            }
            throw ErrorException(ErrorType.COUPON_ISSUE_REDIS_ERROR, compensationError)
        }
    }
}
