package com.coupon.controller.coupon.request

import com.coupon.coupon.command.CouponCommand
import com.coupon.coupon.command.CouponPreviewCommand
import com.coupon.enums.coupon.CouponTrafficPolicy
import com.coupon.enums.coupon.CouponType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "쿠폰 요청")
sealed class CouponRequest {
    @Schema(description = "쿠폰 생성 요청")
    data class Create(
        @param:Schema(description = "쿠폰 이름", example = "신규 가입 축하 쿠폰")
        val name: String,
        @param:Schema(description = "쿠폰 타입", example = "FIXED")
        val couponType: CouponType,
        @param:Schema(description = "할인 금액", example = "5000")
        val discountAmount: Long,
        @param:Schema(description = "최대 할인 금액", example = "10000")
        val maxDiscountAmount: Long?,
        @param:Schema(description = "최소 주문 금액", example = "30000")
        val minOrderAmount: Long?,
        @param:Schema(description = "총 수량", example = "1000")
        val totalQuantity: Long,
        @param:Schema(description = "유효 시작일", example = "2024-12-19T00:00:00")
        val availableAt: LocalDateTime,
        @param:Schema(description = "유효 종료일", example = "2024-12-31T23:59:59")
        val endAt: LocalDateTime,
    ) {
        fun toCommand() =
            CouponCommand.Create(
                name = name,
                couponType = couponType,
                discountAmount = discountAmount,
                maxDiscountAmount = maxDiscountAmount,
                minOrderAmount = minOrderAmount,
                totalQuantity = totalQuantity,
                trafficPolicy = CouponTrafficPolicy.HOT_FCFS_ASYNC,
                availableAt = availableAt,
                endAt = endAt,
            )
    }

    @Schema(description = "쿠폰 수정 요청")
    data class Update(
        @param:Schema(description = "쿠폰 이름", example = "신규 가입 축하 쿠폰")
        val name: String?,
        @param:Schema(description = "할인 금액", example = "5000")
        val discountAmount: Long?,
        @param:Schema(description = "최대 할인 금액", example = "10000")
        val maxDiscountAmount: Long?,
        @param:Schema(description = "최소 주문 금액", example = "30000")
        val minOrderAmount: Long?,
        @param:Schema(description = "유효 시작일", example = "2024-12-19T00:00:00")
        val availableAt: LocalDateTime?,
        @param:Schema(description = "유효 종료일", example = "2024-12-31T23:59:59")
        val endAt: LocalDateTime?,
    ) {
        fun toCommand() =
            CouponCommand.Update(
                name = name,
                discountAmount = discountAmount,
                maxDiscountAmount = maxDiscountAmount,
                minOrderAmount = minOrderAmount,
                trafficPolicy = null,
                availableAt = availableAt,
                endAt = endAt,
            )
    }

    @Schema(description = "쿠폰 미리보기 요청")
    data class Preview(
        @param:Schema(description = "주문 금액", example = "42000")
        val orderAmount: Long,
    ) {
        fun toCommand(
            couponId: Long,
            userId: Long,
        ) = CouponPreviewCommand(
            couponId = couponId,
            userId = userId,
            orderAmount = orderAmount,
        )
    }
}
