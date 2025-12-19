package com.coupon.controller.coupon.response

import com.coupon.coupon.CouponIssueDetail
import com.coupon.enums.CouponIssueStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "쿠폰 발급 응답")
sealed class CouponIssueResponse {
    @Schema(description = "쿠폰 발급 상세 응답")
    data class Detail(
        @param:Schema(description = "쿠폰 발급 ID", example = "1")
        val id: Long,
        @param:Schema(description = "쿠폰 ID", example = "1")
        val couponId: Long,
        @param:Schema(description = "쿠폰 코드", example = "20241219_CP_a1b2c3d4e5f6")
        val couponCode: String,
        @param:Schema(description = "쿠폰 이름", example = "신규 가입 축하 쿠폰")
        val couponName: String,
        @param:Schema(description = "사용자 ID", example = "1")
        val userId: Long,
        @param:Schema(description = "발급 상태", example = "ISSUED")
        val status: CouponIssueStatus,
        @param:Schema(description = "발급일", example = "2024-12-19T10:00:00")
        val issuedAt: LocalDateTime,
        @param:Schema(description = "사용일", example = "2024-12-20T15:30:00")
        val usedAt: LocalDateTime?,
        @param:Schema(description = "취소일", example = "2024-12-21T12:00:00")
        val canceledAt: LocalDateTime?,
    ) {
        companion object {
            fun from(detail: CouponIssueDetail) =
                Detail(
                    id = detail.id,
                    couponId = detail.couponId,
                    couponCode = detail.couponCode,
                    couponName = detail.couponName,
                    userId = detail.userId,
                    status = detail.status,
                    issuedAt = detail.issuedAt,
                    usedAt = detail.usedAt,
                    canceledAt = detail.canceledAt,
                )
        }
    }
}
