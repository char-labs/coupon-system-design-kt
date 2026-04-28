package com.coupon.controller.coupon

import com.coupon.config.ADMIN_ONLY
import com.coupon.controller.coupon.request.CouponRequest
import com.coupon.controller.coupon.response.CouponIssueStateResponse
import com.coupon.controller.coupon.response.CouponPageResponse
import com.coupon.controller.coupon.response.CouponResponse
import com.coupon.coupon.CouponIssueService
import com.coupon.coupon.CouponService
import com.coupon.shared.page.OffsetPageRequest
import com.coupon.user.User
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Coupon API", description = "쿠폰 관리 API")
@RestController
@RequestMapping("/coupons")
class CouponController(
    private val couponService: CouponService,
    private val couponIssueService: CouponIssueService,
) {
    @Operation(summary = "쿠폰 생성", description = "새로운 쿠폰을 생성합니다. (관리자 전용)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ADMIN_ONLY)
    fun createCoupon(
        @Parameter(hidden = true) user: User,
        @RequestBody request: CouponRequest.Create,
    ): CouponResponse.Detail {
        val coupon = couponService.createCoupon(request.toCommand())
        val detail = couponService.getCoupon(coupon.id)
        return CouponResponse.Detail.from(detail)
    }

    @Operation(summary = "쿠폰 목록 조회", description = "쿠폰 목록을 페이징하여 조회합니다.")
    @GetMapping
    fun getCoupons(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): CouponPageResponse = CouponPageResponse.from(couponService.getCoupons(OffsetPageRequest(page, size)))

    @Operation(summary = "쿠폰 상세 조회", description = "쿠폰 상세 정보를 조회합니다.")
    @GetMapping("/{couponId}")
    fun getCoupon(
        @PathVariable couponId: Long,
    ): CouponResponse.Detail = CouponResponse.Detail.from(couponService.getCoupon(couponId))

    @Operation(summary = "쿠폰 미리보기", description = "현재 사용자 기준으로 쿠폰 적용 가능 여부와 예상 할인 금액을 조회합니다.")
    @PostMapping("/{couponId}/preview")
    fun previewCoupon(
        @Parameter(hidden = true) user: User,
        @PathVariable couponId: Long,
        @RequestBody request: CouponRequest.Preview,
    ): CouponResponse.Preview =
        CouponResponse.Preview.from(
            couponService.preview(
                request.toCommand(couponId = couponId, userId = user.id),
            ),
        )

    @Operation(summary = "쿠폰 수정", description = "쿠폰 정보를 수정합니다. (관리자 전용)")
    @PutMapping("/{couponId}")
    @PreAuthorize(ADMIN_ONLY)
    fun modifyCoupon(
        @Parameter(hidden = true) user: User,
        @PathVariable couponId: Long,
        @RequestBody request: CouponRequest.Update,
    ): CouponResponse.Detail = CouponResponse.Detail.from(couponService.modifyCoupon(couponId, request.toCommand()))

    @Operation(summary = "쿠폰 활성화", description = "쿠폰을 활성화합니다. (관리자 전용)")
    @PostMapping("/{couponId}/activate")
    @PreAuthorize(ADMIN_ONLY)
    fun activateCoupon(
        @Parameter(hidden = true) user: User,
        @PathVariable couponId: Long,
    ) = couponService.activateCoupon(couponId)

    @Operation(summary = "쿠폰 비활성화", description = "쿠폰을 비활성화합니다. (관리자 전용)")
    @PostMapping("/{couponId}/deactivate")
    @PreAuthorize(ADMIN_ONLY)
    fun deactivateCoupon(
        @Parameter(hidden = true) user: User,
        @PathVariable couponId: Long,
    ) = couponService.deactivateCoupon(couponId)

    @Operation(summary = "쿠폰 삭제", description = "쿠폰을 삭제합니다. (관리자 전용)")
    @DeleteMapping("/{couponId}")
    @PreAuthorize(ADMIN_ONLY)
    fun deleteCoupon(
        @Parameter(hidden = true) user: User,
        @PathVariable couponId: Long,
    ) = couponService.deleteCoupon(couponId)

    @Operation(summary = "쿠폰 발급 Redis 상태 진단", description = "쿠폰 발급 Redis 상태와 DB 기준 점유 수를 조회합니다. (관리자 전용)")
    @GetMapping("/{couponId}/issue-state")
    @PreAuthorize(ADMIN_ONLY)
    fun diagnoseIssueState(
        @Parameter(hidden = true) user: User,
        @PathVariable couponId: Long,
    ): CouponIssueStateResponse {
        val coupon = couponService.getCoupon(couponId)
        return CouponIssueStateResponse.from(couponIssueService.diagnoseIssueState(coupon))
    }

    @Operation(summary = "쿠폰 발급 Redis 상태 재생성", description = "DB 기준으로 쿠폰 발급 Redis 상태를 명시적으로 재생성합니다. (관리자 전용)")
    @PostMapping("/{couponId}/issue-state/rebuild")
    @PreAuthorize(ADMIN_ONLY)
    fun rebuildIssueState(
        @Parameter(hidden = true) user: User,
        @PathVariable couponId: Long,
    ): CouponIssueStateResponse {
        val coupon = couponService.getCoupon(couponId)
        return CouponIssueStateResponse.from(couponIssueService.rebuildIssueState(coupon))
    }
}
