package com.coupon.controller.loadtest

import com.coupon.bootstrap.LocalAdminBootstrap
import com.coupon.controller.auth.response.TokenResponse
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Hidden
@Profile("local", "load-test")
@RestController
@RequestMapping("/load-test/admin")
class LoadTestAdminController(
    private val localAdminBootstrap: LocalAdminBootstrap,
) {
    @PostMapping("/signin")
    fun signin(): TokenResponse = TokenResponse.from(localAdminBootstrap.signIn())
}
