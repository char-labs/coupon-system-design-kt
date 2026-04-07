package com.coupon.controller.loadtest

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@Profile("local", "load-test")
@RestController
@RequestMapping("/load-test/users")
class LoadTestUserController(
    private val loadTestSyntheticUserService: LoadTestSyntheticUserService,
) {
    /**
     * Bulk-prepares synthetic users before a burst scenario starts.
     * The measured window should focus on coupon issuance, not on creating user rows.
     */
    @PostMapping("/prepare")
    fun prepareUsers(
        @RequestBody request: LoadTestSyntheticUserPrepareMessage,
    ): LoadTestSyntheticUserPrepareResponse =
        LoadTestSyntheticUserPrepareResponse.from(
            loadTestSyntheticUserService.prepareUsers(
                startSequence = request.startSequence,
                count = request.count,
            ),
        )
}
