package com.coupon.coupon.restaurant

import com.coupon.coupon.CouponIssueFacade
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.enums.coupon.CouponIssueResult
import org.springframework.stereotype.Service

@Service
class RestaurantCouponFacade(
    private val restaurantCouponService: RestaurantCouponService,
    private val couponIssueFacade: CouponIssueFacade,
) {
    fun issueByRestaurant(
        restaurantId: Long,
        userId: Long,
    ): CouponIssueResult {
        val restaurantCoupon = restaurantCouponService.getActiveRestaurantCoupon(restaurantId)
        return couponIssueFacade.issue(
            CouponIssueCommand.Issue(
                couponId = restaurantCoupon.couponId,
                userId = userId,
            ),
        )
    }
}
