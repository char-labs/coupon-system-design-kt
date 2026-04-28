package com.coupon.controller.user

import com.coupon.config.ADMIN_ONLY
import com.coupon.config.OWNER_OR_ADMIN
import com.coupon.controller.user.request.UserRequest
import com.coupon.controller.user.response.UserPageResponse
import com.coupon.controller.user.response.UserResponse
import com.coupon.shared.page.OffsetPageRequest
import com.coupon.user.User
import com.coupon.user.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User API", description = "사용자 관련 API")
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {
    @Operation(summary = "어드민 사용자 조회(페이징)", description = "어드민 권한으로 사용자 목록을 조회합니다.")
    @GetMapping
    @PreAuthorize(ADMIN_ONLY)
    fun getUsers(
        @Parameter(hidden = true) user: User,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): UserPageResponse = UserPageResponse.from(userService.getUsers(OffsetPageRequest(page, size)))

    @Operation(summary = "사용자 조회", description = "사용자 정보를 조회합니다.")
    @GetMapping("/{userId}")
    @PreAuthorize(OWNER_OR_ADMIN)
    fun getUser(
        @Parameter(hidden = true) user: User,
        @PathVariable userId: Long,
    ): UserResponse.Profile = UserResponse.Profile.from(userService.getProfile(userId))

    @Operation(summary = "사용자 정보 수정", description = "사용자 정보를 수정합니다.")
    @PutMapping("/{userId}")
    @PreAuthorize(OWNER_OR_ADMIN)
    fun modifyUser(
        @Parameter(hidden = true) user: User,
        @PathVariable userId: Long,
        @RequestBody request: UserRequest.Update,
    ): UserResponse.Profile = UserResponse.Profile.from(userService.modifyUser(userId, request.toCommand()))

    @Operation(summary = "사용자 삭제", description = "사용자 데이터를 삭제합니다.")
    @DeleteMapping("/{userId}")
    @PreAuthorize(ADMIN_ONLY)
    fun deleteUser(
        @Parameter(hidden = true) user: User,
        @PathVariable userId: Long,
    ) = userService.deleteUser(userId)
}
