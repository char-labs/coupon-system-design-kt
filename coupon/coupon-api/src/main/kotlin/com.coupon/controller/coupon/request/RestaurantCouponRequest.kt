package com.coupon.controller.coupon.request

import com.coupon.coupon.restaurant.command.RestaurantCouponCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "맛집 쿠폰 요청")
sealed class RestaurantCouponRequest {
    @Schema(description = "맛집 쿠폰 생성 요청")
    data class Create(
        @param:Schema(description = "맛집 ID", example = "1")
        val restaurantId: Long,
        @param:Schema(description = "쿠폰 ID", example = "1")
        val couponId: Long,
        @param:Schema(description = "유효 시작일", example = "2026-04-08T00:00:00")
        val availableAt: LocalDateTime,
        @param:Schema(description = "유효 종료일", example = "2026-04-08T23:59:59")
        val endAt: LocalDateTime,
    ) {
        fun toCommand() =
            RestaurantCouponCommand.Create(
                restaurantId = restaurantId,
                couponId = couponId,
                availableAt = availableAt,
                endAt = endAt,
            )
    }

    @Schema(description = "맛집 쿠폰 배치 생성 요청")
    data class CreateBatch(
        @param:Schema(description = "맛집 쿠폰 생성 목록 (최대 3건)")
        val items: List<Create>,
    ) {
        fun toCommand() =
            RestaurantCouponCommand.CreateBatch(
                items = items.map { it.toCommand() },
            )
    }

    @Schema(description = "맛집 쿠폰 발급 요청")
    data class IssueByRestaurant(
        @param:Schema(description = "맛집 ID", example = "1")
        val restaurantId: Long,
    )
}
