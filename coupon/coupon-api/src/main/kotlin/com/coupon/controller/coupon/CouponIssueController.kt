package com.coupon.controller.coupon

import com.coupon.controller.coupon.request.CouponIssueRequest
import com.coupon.controller.coupon.response.CouponIssueMessageResponse
import com.coupon.controller.coupon.response.CouponIssuePageResponse
import com.coupon.controller.coupon.response.CouponIssueResponse
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.execution.CouponIssueExecutionFacade
import com.coupon.coupon.intake.CouponIssueIntakeFacade
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.shared.page.OffsetPageRequest
import com.coupon.user.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Coupon Issue API", description = "쿠폰 발급 관련 API")
@RestController
@RequestMapping("/coupon-issues")
class CouponIssueController(
    private val couponIssueIntakeFacade: CouponIssueIntakeFacade,
    private val couponIssueExecutionFacade: CouponIssueExecutionFacade,
    private val couponIssueService: CouponIssueService,
) {
    @Operation(
        summary = "쿠폰 발급 요청",
        description =
            "쿠폰 발급을 즉시 판정하고 ApiResponse.data.result 에 SUCCESS, DUPLICATE, SOLD_OUT 중 하나를 반환합니다. " +
                "SUCCESS는 Redis reserve와 Kafka broker ack 완료를 의미하며, 최종 발급 row는 worker가 비동기로 반영합니다.",
    )
    @PostMapping
    fun issueCoupon(
        @Parameter(hidden = true) user: User,
        @RequestBody request: CouponIssueRequest,
    ): ResponseEntity<CouponIssueMessageResponse> {
        val result = couponIssueIntakeFacade.issue(request.toCommand(user.id))
        val status = if (result == CouponIssueResult.SUCCESS) HttpStatus.ACCEPTED else HttpStatus.OK

        return ResponseEntity
            .status(status)
            .body(CouponIssueMessageResponse.of(result))
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

    @Operation(summary = "쿠폰별 발급 목록 조회", description = "특정 쿠폰의 발급 목록을 페이징하여 조회합니다. (관리자 전용)")
    @GetMapping("/coupons/{couponId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    fun getCouponIssues(
        @Parameter(hidden = true) user: User,
        @PathVariable couponId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): CouponIssuePageResponse =
        CouponIssuePageResponse.from(
            couponIssueService.getCouponIssues(couponId, OffsetPageRequest(page, size)),
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
            couponIssueExecutionFacade.useCoupon(
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
            couponIssueExecutionFacade.cancelCoupon(
                CouponIssueCommand.Cancel(couponIssueId, user.id),
            ),
        )
}
