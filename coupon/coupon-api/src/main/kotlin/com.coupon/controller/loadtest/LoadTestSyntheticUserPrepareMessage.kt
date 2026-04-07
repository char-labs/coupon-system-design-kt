package com.coupon.controller.loadtest

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Load test synthetic user bulk prepare request")
data class LoadTestSyntheticUserPrepareMessage(
    @param:Schema(description = "Synthetic user sequence start", example = "1000000")
    val startSequence: Long,
    @param:Schema(description = "How many synthetic users should be prepared", example = "1000")
    val count: Int,
)
