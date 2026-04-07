package com.coupon.controller.loadtest

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Load test synthetic coupon issue request")
data class LoadTestCouponIssueRequestMessage(
    @param:Schema(description = "Synthetic user ID used only for local load testing", example = "1000001")
    val userId: Long,
)
