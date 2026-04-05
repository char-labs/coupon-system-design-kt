package com.coupon.controller.coupon.response

import com.coupon.coupon.CouponDetail
import com.coupon.coupon.CouponPreview
import com.coupon.enums.coupon.CouponPreviewInapplicableReason
import com.coupon.enums.coupon.CouponStatus
import com.coupon.enums.coupon.CouponType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "쿠폰 응답")
sealed class CouponResponse {
    @Schema(description = "쿠폰 상세 응답")
    data class Detail(
        @param:Schema(description = "쿠폰 ID", example = "1")
        val id: Long,
        @param:Schema(description = "쿠폰 코드", example = "20241219_CP_a1b2c3d4e5f6")
        val code: String,
        @param:Schema(description = "쿠폰 이름", example = "신규 가입 축하 쿠폰")
        val name: String,
        @param:Schema(description = "쿠폰 타입", example = "FIXED")
        val type: CouponType,
        @param:Schema(description = "쿠폰 상태", example = "ACTIVE")
        val status: CouponStatus,
        @param:Schema(description = "할인 금액", example = "5000")
        val discountAmount: Long,
        @param:Schema(description = "최대 할인 금액", example = "10000")
        val maxDiscountAmount: Long?,
        @param:Schema(description = "최소 주문 금액", example = "30000")
        val minOrderAmount: Long?,
        @param:Schema(description = "총 수량", example = "1000")
        val totalQuantity: Long,
        @param:Schema(description = "남은 수량", example = "850")
        val remainingQuantity: Long,
        @param:Schema(description = "유효 시작일", example = "2024-12-19T00:00:00")
        val availableAt: LocalDateTime,
        @param:Schema(description = "유효 종료일", example = "2024-12-31T23:59:59")
        val endAt: LocalDateTime,
        @param:Schema(description = "생성일", example = "2024-12-19T10:00:00")
        val createdAt: LocalDateTime,
        @param:Schema(description = "수정일", example = "2024-12-19T11:00:00")
        val updatedAt: LocalDateTime?,
    ) {
        companion object {
            fun from(detail: CouponDetail) =
                Detail(
                    id = detail.id,
                    code = detail.code,
                    name = detail.name,
                    type = detail.type,
                    status = detail.status,
                    discountAmount = detail.discountAmount,
                    maxDiscountAmount = detail.maxDiscountAmount,
                    minOrderAmount = detail.minOrderAmount,
                    totalQuantity = detail.totalQuantity,
                    remainingQuantity = detail.remainingQuantity,
                    availableAt = detail.availableAt,
                    endAt = detail.endAt,
                    createdAt = detail.createdAt,
                    updatedAt = detail.updatedAt,
                )
        }
    }

    @Schema(description = "쿠폰 미리보기 응답")
    data class Preview(
        @param:Schema(description = "쿠폰 ID", example = "1")
        val couponId: Long,
        @param:Schema(description = "쿠폰 코드", example = "20241219_CP_a1b2c3d4e5f6")
        val couponCode: String,
        @param:Schema(description = "쿠폰 이름", example = "신규 가입 축하 쿠폰")
        val couponName: String,
        @param:Schema(description = "쿠폰 타입", example = "FIXED")
        val couponType: CouponType,
        @param:Schema(description = "주문 금액", example = "42000")
        val orderAmount: Long,
        @param:Schema(description = "적용 가능 여부", example = "true")
        val applicable: Boolean,
        @param:Schema(description = "예상 할인 금액", example = "5000")
        val discountAmount: Long,
        @param:Schema(description = "적용 불가 사유", example = "ALREADY_ISSUED")
        val reason: CouponPreviewInapplicableReason?,
    ) {
        companion object {
            fun from(preview: CouponPreview) =
                Preview(
                    couponId = preview.couponId,
                    couponCode = preview.couponCode,
                    couponName = preview.couponName,
                    couponType = preview.couponType,
                    orderAmount = preview.orderAmount,
                    applicable = preview.applicable,
                    discountAmount = preview.discountAmount,
                    reason = preview.reason,
                )
        }
    }
}
