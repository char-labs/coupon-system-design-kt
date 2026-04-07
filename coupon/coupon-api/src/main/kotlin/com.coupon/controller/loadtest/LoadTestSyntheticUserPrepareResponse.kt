package com.coupon.controller.loadtest

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Load test synthetic user bulk prepare response")
data class LoadTestSyntheticUserPrepareResponse(
    @param:Schema(description = "Synthetic user sequence start", example = "1000000")
    val startSequence: Long,
    @param:Schema(description = "Requested synthetic user count", example = "1000")
    val requestedCount: Int,
    @param:Schema(description = "How many users were newly created", example = "1000")
    val preparedCount: Int,
    @param:Schema(description = "How many users already existed", example = "0")
    val existingCount: Int,
) {
    companion object {
        fun from(preparation: LoadTestSyntheticUserService.SyntheticUserPreparation) =
            LoadTestSyntheticUserPrepareResponse(
                startSequence = preparation.startSequence,
                requestedCount = preparation.requestedCount,
                preparedCount = preparation.preparedCount,
                existingCount = preparation.existingCount,
            )
    }
}
