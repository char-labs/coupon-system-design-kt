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

@Service
class CouponIssueFacade(
    private val couponService: CouponService,
    private val couponIssueService: CouponIssueService,
) {
    private val log by logger()

    fun issue(command: CouponIssueCommand.Issue): CouponIssueResult {
        val coupon = couponService.getAvailableCouponForIssue(command.couponId)
        val result = couponIssueService.reserveIssue(coupon, command.userId)
        if (result != CouponIssueResult.SUCCESS) {
            return result
        }

        try {
            couponIssueService.publishIssue(command.couponId, command.userId)
            return CouponIssueResult.SUCCESS
        } catch (exception: Exception) {
            runCatching { couponIssueService.release(command.couponId, command.userId) }
                .onFailure { compensationError ->
                    log.error(compensationError) {
                        "Failed to release coupon issue state after Kafka publish failure. couponId=${command.couponId}, userId=${command.userId}"
                    }
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

    fun execute(message: CouponIssueMessage): CouponIssueAsyncExecutionResult =
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
}
