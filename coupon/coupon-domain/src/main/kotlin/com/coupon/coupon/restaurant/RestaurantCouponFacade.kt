package com.coupon.coupon.restaurant

import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.intake.CouponIssueIntakeFacade
import com.coupon.enums.coupon.CouponIssueResult
import org.springframework.stereotype.Service

@Service
class RestaurantCouponFacade(
    private val restaurantCouponService: RestaurantCouponService,
    private val couponIssueIntakeFacade: CouponIssueIntakeFacade,
) {
    /**
     * 맛집 쿠폰 발급은 얇은 adapter 역할만 맡는다.
     * 먼저 현재 발급 가능한 맛집 쿠폰을 찾고,
     * 그 couponId를 일반 쿠폰 발급 흐름에 그대로 넘긴다.
     */
    fun issueByRestaurant(
        restaurantId: Long,
        userId: Long,
    ): CouponIssueResult {
        val restaurantCoupon = restaurantCouponService.getActiveRestaurantCoupon(restaurantId)
        return couponIssueIntakeFacade.issue(
            CouponIssueCommand.Issue(
                couponId = restaurantCoupon.couponId,
                userId = userId,
            ),
        )
    }
}
