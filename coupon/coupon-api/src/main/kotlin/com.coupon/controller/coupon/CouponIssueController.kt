package com.coupon.controller.coupon

import com.coupon.controller.coupon.request.CouponIssueRequest
import com.coupon.controller.coupon.response.CouponIssuePageResponse
import com.coupon.controller.coupon.response.CouponIssueResponse
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.support.page.OffsetPageRequest
import com.coupon.user.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Coupon Issue API", description = "쿠폰 발급 관련 API")
@RestController
@RequestMapping("/coupon-issues")
class CouponIssueController(
    private val couponIssueService: CouponIssueService,
) {
    @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰을 발급합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun issueCoupon(
        @Parameter(hidden = true) user: User,
        @RequestBody request: CouponIssueRequest.Issue,
    ): CouponIssueResponse.Detail {
        val couponIssue = couponIssueService.issueCoupon(request.toCommand(user.id))
        val detail = couponIssueService.getCouponIssue(couponIssue.id)
        return CouponIssueResponse.Detail.from(detail)
    }

    @Operation(summary = "내 쿠폰 목록 조회", description = "내가 발급받은 쿠폰 목록을 조회합니다.")
    @GetMapping("/my")
    fun getMyCoupons(
        @Parameter(hidden = true) user: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): CouponIssuePageResponse =
        CouponIssuePageResponse.from(
            couponIssueService.getMyCoupons(user.id, OffsetPageRequest(page, size)),
        )

    @Operation(summary = "쿠폰 발급 상세 조회", description = "쿠폰 발급 상세 정보를 조회합니다.")
    @GetMapping("/{couponIssueId}")
    fun getCouponIssue(
        @PathVariable couponIssueId: Long,
    ): CouponIssueResponse.Detail = CouponIssueResponse.Detail.from(couponIssueService.getCouponIssue(couponIssueId))

    @Operation(summary = "쿠폰 사용", description = "발급받은 쿠폰을 사용합니다.")
    @PostMapping("/{couponIssueId}/use")
    fun useCoupon(
        @Parameter(hidden = true) user: User,
        @PathVariable couponIssueId: Long,
    ): CouponIssueResponse.Detail =
        CouponIssueResponse.Detail.from(
            couponIssueService.useCoupon(
                CouponIssueCommand.Use(couponIssueId, user.id),
            ),
        )

    @Operation(summary = "쿠폰 취소", description = "발급받은 쿠폰을 취소합니다.")
    @PostMapping("/{couponIssueId}/cancel")
    fun cancelCoupon(
        @Parameter(hidden = true) user: User,
        @PathVariable couponIssueId: Long,
    ): CouponIssueResponse.Detail =
        CouponIssueResponse.Detail.from(
            couponIssueService.cancelCoupon(
                CouponIssueCommand.Cancel(couponIssueId, user.id),
            ),
        )
}
