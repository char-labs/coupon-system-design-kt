package com.coupon.coupon.execution

import com.coupon.coupon.CouponIssue
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.intake.CouponIssueMessage
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import org.springframework.stereotype.Service

@Service
class CouponIssueExecutionFacade(
    private val couponIssueService: CouponIssueService,
    private val couponIssueLockingExecutor: CouponIssueLockingExecutor,
    private val couponIssueCancellationExecutor: CouponIssueCancellationExecutor,
    private val couponIssueUsageExecutor: CouponIssueUsageExecutor,
) {
    fun executeIssue(command: CouponIssueCommand.Issue): CouponIssue = couponIssueLockingExecutor.executeIssue(command)

    /**
     * worker consume 결과를 여기서 한 번에 정리한다.
     * 성공, 즉시 거절, 재시도 여부를 분기하고
     * 거절 가능한 경우에만 Redis reserve를 해제한다.
     */
    fun execute(message: CouponIssueMessage): CouponIssueExecutionResult {
        val command =
            CouponIssueCommand.Issue(
                couponId = message.couponId,
                userId = message.userId,
            )

        return try {
            val couponIssue = executeIssue(command)
            CouponIssueExecutionResult.Succeeded(couponIssue.id)
        } catch (exception: ErrorException) {
            when (exception.errorType) {
                ErrorType.LOCK_ACQUISITION_FAILED ->
                    CouponIssueExecutionResult.Retry(
                        reason = "${exception::class.simpleName}: ${exception.message ?: "알 수 없는 오류"}",
                    )

                ErrorType.ALREADY_ISSUED_COUPON -> CouponIssueExecutionResult.AlreadyIssued

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

                    CouponIssueExecutionResult.Rejected(exception.errorType)
                }

                else ->
                    CouponIssueExecutionResult.Retry(
                        reason = "${exception::class.simpleName}: ${exception.message ?: "알 수 없는 오류"}",
                    )
            }
        } catch (exception: Exception) {
            CouponIssueExecutionResult.Retry(
                reason = "${exception::class.simpleName}: ${exception.message ?: "알 수 없는 오류"}",
            )
        }
    }

    fun cancelCoupon(command: CouponIssueCommand.Cancel): CouponIssue.Detail {
        val couponIssue = couponIssueService.getCouponIssue(command.couponIssueId)
        return couponIssueCancellationExecutor.cancelCoupon(couponIssue, command)
    }

    fun useCoupon(command: CouponIssueCommand.Use): CouponIssue.Detail = couponIssueUsageExecutor.useCoupon(command)
}
