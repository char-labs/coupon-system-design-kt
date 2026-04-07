package com.coupon.controller.loadtest

import com.coupon.controller.coupon.response.CouponIssueRequestResponse
import com.coupon.coupon.request.CouponIssueRequestService
import com.coupon.coupon.request.command.CouponIssueRequestCommand
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Hidden
@Profile("local", "load-test")
@RestController
@RequestMapping("/load-test")
class LoadTestCouponIssueRequestController(
    private val couponIssueRequestService: CouponIssueRequestService,
    private val loadTestSyntheticUserService: LoadTestSyntheticUserService,
) {
    /**
     * Synthetic acceptance endpoint used only for local k6 scenarios.
     * It bypasses JWT auth so the test can focus on request intake throughput instead of signup/signin overhead.
     */
    @PostMapping("/coupons/{couponId}/issue-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestIssueCoupon(
        @PathVariable couponId: Long,
        @RequestBody request: LoadTestCouponIssueRequestMessage,
    ): CouponIssueRequestResponse =
        CouponIssueRequestResponse.from(
            couponIssueRequestService.accept(
                CouponIssueRequestCommand.Accept(
                    couponId = couponId,
                    userId = loadTestSyntheticUserService.resolveUserId(request.userId),
                ),
            ),
        )

    /**
     * Synthetic status endpoint paired with the local acceptance endpoint.
     * It intentionally skips owner checks because synthetic user IDs do not go through normal authentication.
     */
    @GetMapping("/coupon-issue-requests/{requestId}")
    fun getCouponIssueRequest(
        @PathVariable requestId: Long,
    ): CouponIssueRequestResponse =
        CouponIssueRequestResponse.from(
            couponIssueRequestService.getById(requestId),
        )
}
