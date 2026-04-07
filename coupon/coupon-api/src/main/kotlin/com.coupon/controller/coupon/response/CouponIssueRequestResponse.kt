package com.coupon.controller.coupon.response

import com.coupon.coupon.request.CouponIssueRequest
import com.coupon.enums.coupon.CouponCommandResultCode
import com.coupon.enums.coupon.CouponIssueRequestStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "쿠폰 발급 요청 상태 응답")
data class CouponIssueRequestResponse(
    @param:Schema(description = "요청 ID", example = "1")
    val id: Long,
    @param:Schema(description = "쿠폰 ID", example = "1")
    val couponId: Long,
    @param:Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    @param:Schema(description = "요청 상태", example = "PENDING")
    val status: CouponIssueRequestStatus,
    @param:Schema(description = "처리 결과 코드", example = "OUT_OF_STOCK")
    val resultCode: CouponCommandResultCode?,
    @param:Schema(description = "발급된 쿠폰 ID", example = "101")
    val couponIssueId: Long?,
    @param:Schema(description = "실패 사유", example = "쿠폰 수량이 소진되었습니다.")
    val failureReason: String?,
    @param:Schema(description = "처리 시각", example = "2026-04-07T10:00:00")
    val processedAt: LocalDateTime?,
    @param:Schema(description = "요청 생성 시각", example = "2026-04-07T09:59:59")
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(request: CouponIssueRequest) =
            CouponIssueRequestResponse(
                id = request.id,
                couponId = request.couponId,
                userId = request.userId,
                status = request.status,
                resultCode = request.resultCode,
                couponIssueId = request.couponIssueId,
                failureReason = request.failureReason,
                processedAt = request.processedAt,
                createdAt = request.createdAt,
            )
    }
}
