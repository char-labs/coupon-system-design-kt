package com.coupon.coupon

import com.coupon.coupon.CouponIssueAsyncExecutionResult.AlreadyIssued
import com.coupon.coupon.CouponIssueAsyncExecutionResult.Rejected
import com.coupon.coupon.CouponIssueAsyncExecutionResult.Retry
import com.coupon.coupon.CouponIssueAsyncExecutionResult.Succeeded
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.lock.Lock
import com.coupon.support.logging.logger
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class CouponIssueFacade(
    private val couponService: CouponService,
    private val couponIssueService: CouponIssueService,
    private val clock: Clock,
) {
    private val log by logger()

    fun issue(command: CouponIssueCommand.Issue): CouponIssueResult {
        val requestId = UUID.randomUUID().toString()
        val coupon = couponService.getAvailableCouponForIssue(command.couponId)
        val result = couponIssueService.reserveIssue(coupon, command.userId)
        log.info {
            "event=coupon.issue phase=intake.reserve result=${result.name} requestId=$requestId " +
                "couponId=${command.couponId} userId=${command.userId} errorType=NONE"
        }
        if (result != CouponIssueResult.SUCCESS) {
            return result
        }

        val message =
            CouponIssueMessage(
                couponId = command.couponId,
                userId = command.userId,
                requestId = requestId,
                acceptedAt = Instant.now(clock),
            )
        val publishStartedAt = System.nanoTime()

        try {
            val receipt = couponIssueService.publishIssue(message)
            val publishDuration = elapsedSince(publishStartedAt)
            log.info {
                "event=coupon.issue phase=intake.publish result=SUCCESS requestId=${message.requestId} " +
                    "couponId=${message.couponId} userId=${message.userId} " +
                    "acceptedAt=${message.acceptedAt} durationMs=${publishDuration.toMillis()} " +
                    "errorType=NONE topic=${receipt.topic} partition=${receipt.partition} offset=${receipt.offset}"
            }
            return CouponIssueResult.SUCCESS
        } catch (exception: Exception) {
            val publishDuration = elapsedSince(publishStartedAt)
            runCatching { couponIssueService.release(command.couponId, command.userId) }
                .onFailure { compensationError ->
                    log.error(compensationError) {
                        "event=coupon.issue phase=intake.compensation result=FAILURE requestId=${message.requestId} " +
                            "couponId=${command.couponId} userId=${command.userId} " +
                            "errorType=${ErrorType.COUPON_ISSUE_KAFKA_ERROR.name}"
                    }
                }

            log.error(exception) {
                "event=coupon.issue phase=intake.publish result=FAILURE requestId=${message.requestId} " +
                    "couponId=${message.couponId} userId=${message.userId} " +
                    "acceptedAt=${message.acceptedAt} durationMs=${publishDuration.toMillis()} " +
                    "errorType=${ErrorType.COUPON_ISSUE_KAFKA_ERROR.name}"
            }
            throw ErrorException(ErrorType.COUPON_ISSUE_KAFKA_ERROR)
        }
    }

    fun executeIssue(command: CouponIssueCommand.Issue): CouponIssue =
        Lock.executeWithLockRequiresNew(
            key = "COUPON_ISSUE:${command.couponId}",
        ) {
            couponService.validateAvailability(command.couponId)
            val decreased = couponService.decreaseQuantityIfAvailable(command.couponId)
            if (!decreased) {
                throw ErrorException(ErrorType.COUPON_OUT_OF_STOCK)
            }

            couponIssueService.executeIssue(command)
        }

    fun execute(message: CouponIssueMessage): CouponIssueAsyncExecutionResult {
        val result =
            try {
                val couponIssue =
                    executeIssue(
                        CouponIssueCommand.Issue(
                            couponId = message.couponId,
                            userId = message.userId,
                        ),
                    )

                Succeeded(couponIssue.id)
            } catch (exception: ErrorException) {
                when (exception.errorType) {
                    ErrorType.LOCK_ACQUISITION_FAILED ->
                        Retry(
                            reason = "${exception::class.simpleName}: ${exception.message ?: "Unknown error"}",
                        )

                    ErrorType.ALREADY_ISSUED_COUPON -> AlreadyIssued

                    ErrorType.COUPON_OUT_OF_STOCK,
                    ErrorType.COUPON_EXPIRED,
                    ErrorType.COUPON_NOT_ACTIVE,
                    ErrorType.NOT_FOUND_DATA,
                    ErrorType.NOT_FOUND_COUPON,
                    ErrorType.INVALID_COUPON_STATUS,
                    ErrorType.FORBIDDEN_COUPON_ISSUE,
                    ErrorType.FORBIDDEN_ACCESS,
                    -> {
                        couponIssueService.release(
                            couponId = message.couponId,
                            userId = message.userId,
                        )

                        Rejected(exception.errorType)
                    }

                    else ->
                        Retry(
                            reason = "${exception::class.simpleName}: ${exception.message ?: "Unknown error"}",
                        )
                }
            } catch (exception: Exception) {
                Retry(
                    reason = "${exception::class.simpleName}: ${exception.message ?: "Unknown error"}",
                )
            }

        return result
    }

    fun cancelCoupon(command: CouponIssueCommand.Cancel): CouponIssue.Detail {
        val couponIssue = couponIssueService.getCouponIssue(command.couponIssueId)

        return Lock.executeWithLockRequiresNew(
            key = "COUPON_ISSUE:${couponIssue.couponId}",
        ) {
            couponIssueService.cancelIssue(command)
            couponService.increaseQuantity(couponIssue.couponId)
            couponIssueService.releaseStockSlot(couponIssue.couponId)
            couponIssueService.getCouponIssue(command.couponIssueId)
        }
    }

    private fun elapsedSince(startedAtNanos: Long): Duration = Duration.ofNanos((System.nanoTime() - startedAtNanos).coerceAtLeast(0))
}
