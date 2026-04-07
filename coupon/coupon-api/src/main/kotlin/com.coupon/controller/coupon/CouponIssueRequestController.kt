package com.coupon.controller.coupon

import com.coupon.controller.coupon.request.CouponIssueRequestMessage
import com.coupon.controller.coupon.response.CouponIssueRequestResponse
import com.coupon.coupon.request.CouponIssueRequestService
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Coupon Issue Request API", description = "쿠폰 발급 요청 수락 및 상태 조회 API")
@RestController
@RequestMapping("/coupon-issue-requests")
class CouponIssueRequestController(
    private val couponIssueRequestService: CouponIssueRequestService,
) {
    @Operation(summary = "쿠폰 발급 요청 접수", description = "쿠폰 발급 요청을 안전하게 접수하고 request 상태를 반환합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestIssueCoupon(
        @Parameter(hidden = true) user: User,
        @RequestBody request: CouponIssueRequestMessage,
    ): CouponIssueRequestResponse =
        CouponIssueRequestResponse.from(
            couponIssueRequestService.accept(request.toCommand(user.id)),
        )

    @Operation(summary = "쿠폰 발급 요청 상태 조회", description = "쿠폰 발급 요청의 현재 상태를 조회합니다.")
    @GetMapping("/{requestId}")
    fun getCouponIssueRequest(
        @Parameter(hidden = true) user: User,
        @PathVariable requestId: Long,
    ): CouponIssueRequestResponse =
        CouponIssueRequestResponse.from(
            couponIssueRequestService.getRequest(
                requestId = requestId,
                userId = user.id,
            ),
        )
}
