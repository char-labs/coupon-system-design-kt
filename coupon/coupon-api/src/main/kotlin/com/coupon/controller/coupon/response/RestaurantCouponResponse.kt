package com.coupon.controller.coupon.response

import com.coupon.coupon.restaurant.RestaurantCoupon
import com.coupon.enums.coupon.RestaurantCouponStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "맛집 쿠폰 응답")
sealed class RestaurantCouponResponse {
    @Schema(description = "맛집 쿠폰 상세 응답")
    data class Detail(
        @param:Schema(description = "맛집 쿠폰 ID", example = "1")
        val id: Long,
        @param:Schema(description = "맛집 ID", example = "1")
        val restaurantId: Long,
        @param:Schema(description = "쿠폰 ID", example = "1")
        val couponId: Long,
        @param:Schema(description = "상태", example = "ACTIVE")
        val status: RestaurantCouponStatus,
        @param:Schema(description = "유효 시작일", example = "2026-04-08T00:00:00")
        val availableAt: LocalDateTime,
        @param:Schema(description = "유효 종료일", example = "2026-04-08T23:59:59")
        val endAt: LocalDateTime,
        @param:Schema(description = "생성일", example = "2026-04-08T10:00:00")
        val createdAt: LocalDateTime,
        @param:Schema(description = "수정일", example = "2026-04-08T11:00:00")
        val updatedAt: LocalDateTime?,
    ) {
        companion object {
            fun from(restaurantCoupon: RestaurantCoupon) =
                Detail(
                    id = restaurantCoupon.id,
                    restaurantId = restaurantCoupon.restaurantId,
                    couponId = restaurantCoupon.couponId,
                    status = restaurantCoupon.status,
                    availableAt = restaurantCoupon.availableAt,
                    endAt = restaurantCoupon.endAt,
                    createdAt = restaurantCoupon.createdAt,
                    updatedAt = restaurantCoupon.updatedAt,
                )
        }
    }
}
