package com.coupon.controller.coupon

import com.coupon.config.ADMIN_ONLY
import com.coupon.controller.coupon.request.RestaurantCouponRequest
import com.coupon.controller.coupon.response.CouponIssueMessageResponse
import com.coupon.controller.coupon.response.RestaurantCouponResponse
import com.coupon.coupon.restaurant.RestaurantCouponFacade
import com.coupon.coupon.restaurant.RestaurantCouponService
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.user.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Restaurant Coupon API", description = "맛집 쿠폰 API")
@RestController
@RequestMapping("/restaurant-coupons")
class RestaurantCouponController(
    private val restaurantCouponService: RestaurantCouponService,
    private val restaurantCouponFacade: RestaurantCouponFacade,
) {
    @Operation(summary = "맛집 쿠폰 배치 생성", description = "오늘의 식당 쿠폰을 배치로 생성합니다. (관리자 전용, 최대 3건)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN_ONLY)
    fun createRestaurantCoupons(
        @Parameter(hidden = true) user: User,
        @RequestBody request: RestaurantCouponRequest.CreateBatch,
    ): List<RestaurantCouponResponse.Detail> =
        restaurantCouponService
            .createRestaurantCoupons(request.toCommand())
            .map { RestaurantCouponResponse.Detail.from(it) }

    @Operation(
        summary = "맛집 쿠폰 발급",
        description = "맛집 ID로 연결된 쿠폰을 발급합니다.",
    )
    @PostMapping("/issue")
    fun issueByRestaurant(
        @Parameter(hidden = true) user: User,
        @RequestBody request: RestaurantCouponRequest.IssueByRestaurant,
    ): ResponseEntity<CouponIssueMessageResponse> {
        val result = restaurantCouponFacade.issueByRestaurant(request.restaurantId, user.id)
        val status = if (result == CouponIssueResult.SUCCESS) HttpStatus.ACCEPTED else HttpStatus.OK

        return ResponseEntity
            .status(status)
            .body(CouponIssueMessageResponse.of(result))
    }

    @Operation(summary = "활성 맛집 쿠폰 목록 조회", description = "현재 활성화된 맛집 쿠폰(오늘의 식당) 목록을 조회합니다.")
    @GetMapping("/active")
    fun getActiveRestaurantCoupons(): List<RestaurantCouponResponse.Detail> =
        restaurantCouponService
            .getActiveRestaurantCoupons()
            .map { RestaurantCouponResponse.Detail.from(it) }

    @Operation(summary = "맛집 쿠폰 삭제", description = "맛집 쿠폰을 삭제합니다. (관리자 전용)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ADMIN_ONLY)
    fun deleteRestaurantCoupon(
        @Parameter(hidden = true) user: User,
        @PathVariable id: Long,
    ) = restaurantCouponService.deleteRestaurantCoupon(id)
}
