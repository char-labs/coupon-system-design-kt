package com.coupon.coupon

import com.coupon.controller.coupon.request.RestaurantCouponRequest
import com.coupon.coupon.restaurant.RestaurantCouponService
import com.coupon.coupon.restaurant.command.RestaurantCouponCommand
import com.coupon.enums.auth.AuthorityType
import com.coupon.enums.error.ErrorType
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.testing.CouponApiWebIntegrationTest
import com.coupon.support.testing.DatabaseCleaner
import com.coupon.user.User
import com.coupon.user.UserRepository
import com.coupon.user.criteria.UserCriteria
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID

@CouponApiWebIntegrationTest
open class RestaurantCouponControllerIntegrationTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val couponService: CouponService,
    private val restaurantCouponService: RestaurantCouponService,
    private val couponIssueService: CouponIssueService,
    private val userRepository: UserRepository,
    private val databaseCleaner: DatabaseCleaner,
) : BehaviorSpec() {
    init {
        beforeTest {
            databaseCleaner.clean()
        }

        given("레스토랑 쿠폰 배치 생성 API") {
            `when`("관리자가 3건을 생성하면") {
                then("201과 생성 목록을 반환한다") {
                    val admin = createUser(index = 1, role = AuthorityType.ADMIN)
                    val coupons = (1..3).map { createCoupon(totalQuantity = 10L, index = it) }
                    val request =
                        RestaurantCouponRequest.CreateBatch(
                            items =
                                coupons.mapIndexed { index, coupon ->
                                    RestaurantCouponRequest.Create(
                                        restaurantId = 100L + index,
                                        couponId = coupon.id,
                                        availableAt = LocalDateTime.now().minusHours(1),
                                        endAt = LocalDateTime.now().plusDays(1),
                                    )
                                },
                        )

                    mockMvc
                        .perform(
                            post("/restaurant-coupons")
                                .with(authentication(authenticationOf(admin)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)),
                        ).andExpect(status().isCreated)
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(201))
                        .andExpect(jsonPath("$.data.length()").value(3))
                        .andExpect(jsonPath("$.data[0].restaurantId").value(100))
                }
            }

            `when`("관리자가 4건을 생성하면") {
                then("400과 배치 크기 에러를 반환한다") {
                    val admin = createUser(index = 2, role = AuthorityType.ADMIN)
                    val coupons = (1..4).map { createCoupon(totalQuantity = 10L, index = 10 + it) }
                    val request =
                        RestaurantCouponRequest.CreateBatch(
                            items =
                                coupons.mapIndexed { index, coupon ->
                                    RestaurantCouponRequest.Create(
                                        restaurantId = 200L + index,
                                        couponId = coupon.id,
                                        availableAt = LocalDateTime.now().minusHours(1),
                                        endAt = LocalDateTime.now().plusDays(1),
                                    )
                                },
                        )

                    mockMvc
                        .perform(
                            post("/restaurant-coupons")
                                .with(authentication(authenticationOf(admin)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)),
                        ).andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.data.errorClassName").value(ErrorType.INVALID_RESTAURANT_COUPON_BATCH_SIZE.name))
                }
            }

            `when`("같은 restaurantId와 couponId를 중복으로 생성하면") {
                then("409를 반환한다") {
                    val admin = createUser(index = 3, role = AuthorityType.ADMIN)
                    val coupon = createCoupon(totalQuantity = 10L, index = 20)
                    val request =
                        RestaurantCouponRequest.CreateBatch(
                            items =
                                listOf(
                                    RestaurantCouponRequest.Create(
                                        restaurantId = 301L,
                                        couponId = coupon.id,
                                        availableAt = LocalDateTime.now().minusHours(1),
                                        endAt = LocalDateTime.now().plusDays(1),
                                    ),
                                ),
                        )

                    mockMvc
                        .perform(
                            post("/restaurant-coupons")
                                .with(authentication(authenticationOf(admin)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)),
                        ).andExpect(status().isCreated)

                    mockMvc
                        .perform(
                            post("/restaurant-coupons")
                                .with(authentication(authenticationOf(admin)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)),
                        ).andExpect(status().isConflict)
                        .andExpect(jsonPath("$.data.errorClassName").value(ErrorType.DUPLICATED_RESTAURANT_COUPON.name))
                }
            }
        }

        given("레스토랑 쿠폰 발급 API") {
            `when`("활성 매핑이 있으면") {
                then("202 SUCCESS와 실제 발급 결과를 남긴다") {
                    val user = createUser(index = 11)
                    val coupon = createCoupon(totalQuantity = 3L, index = 30)
                    restaurantCouponService.createRestaurantCoupon(
                        createRestaurantCouponCommand(
                            restaurantId = 101L,
                            couponId = coupon.id,
                        ),
                    )

                    mockMvc
                        .perform(
                            post("/restaurant-coupons/issue")
                                .with(authentication(authenticationOf(user)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(RestaurantCouponRequest.IssueByRestaurant(restaurantId = 101L))),
                        ).andExpect(status().isAccepted)
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(202))
                        .andExpect(jsonPath("$.data.result").value("SUCCESS"))

                    couponIssueService.getCouponIssues(coupon.id, OffsetPageRequest(0, 100)).totalCount shouldBe 1L
                    couponService.getCoupon(coupon.id).remainingQuantity shouldBe 2L
                }
            }

            `when`("같은 사용자가 같은 restaurant로 다시 발급하면") {
                then("200 DUPLICATE를 반환한다") {
                    val user = createUser(index = 12)
                    val coupon = createCoupon(totalQuantity = 3L, index = 31)
                    restaurantCouponService.createRestaurantCoupon(
                        createRestaurantCouponCommand(
                            restaurantId = 102L,
                            couponId = coupon.id,
                        ),
                    )

                    val request = RestaurantCouponRequest.IssueByRestaurant(restaurantId = 102L)

                    mockMvc
                        .perform(
                            post("/restaurant-coupons/issue")
                                .with(authentication(authenticationOf(user)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)),
                        ).andExpect(status().isAccepted)

                    mockMvc
                        .perform(
                            post("/restaurant-coupons/issue")
                                .with(authentication(authenticationOf(user)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)),
                        ).andExpect(status().isOk)
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.status").value(200))
                        .andExpect(jsonPath("$.data.result").value("DUPLICATE"))
                }
            }

            `when`("등록되지 않은 restaurant면") {
                then("404를 반환한다") {
                    val user = createUser(index = 13)

                    mockMvc
                        .perform(
                            post("/restaurant-coupons/issue")
                                .with(authentication(authenticationOf(user)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(RestaurantCouponRequest.IssueByRestaurant(restaurantId = 999L))),
                        ).andExpect(status().isNotFound)
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.data.errorClassName").value(ErrorType.NOT_FOUND_RESTAURANT_COUPON.name))
                }
            }

            `when`("만료된 매핑이면") {
                then("400을 반환한다") {
                    val user = createUser(index = 14)
                    val coupon = createCoupon(totalQuantity = 3L, index = 32)
                    restaurantCouponService.createRestaurantCoupon(
                        createRestaurantCouponCommand(
                            restaurantId = 103L,
                            couponId = coupon.id,
                            availableAt = LocalDateTime.now().minusDays(2),
                            endAt = LocalDateTime.now().minusMinutes(1),
                        ),
                    )

                    mockMvc
                        .perform(
                            post("/restaurant-coupons/issue")
                                .with(authentication(authenticationOf(user)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(RestaurantCouponRequest.IssueByRestaurant(restaurantId = 103L))),
                        ).andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.data.errorClassName").value(ErrorType.RESTAURANT_COUPON_EXPIRED.name))
                }
            }
        }
    }

    private fun createCoupon(
        totalQuantity: Long,
        index: Int,
    ): Coupon =
        couponService.createCoupon(
            CouponApiFixtures.couponCreateCommand(totalQuantity = totalQuantity).copy(name = "restaurant-coupon-$index"),
        )

    private fun createRestaurantCouponCommand(
        restaurantId: Long,
        couponId: Long,
        availableAt: LocalDateTime = LocalDateTime.now().minusHours(1),
        endAt: LocalDateTime = LocalDateTime.now().plusHours(1),
    ) = RestaurantCouponCommand.Create(
        restaurantId = restaurantId,
        couponId = couponId,
        availableAt = availableAt,
        endAt = endAt,
    )

    private fun createUser(
        index: Int,
        role: AuthorityType = AuthorityType.USER,
    ): User =
        userRepository.save(
            UserCriteria.Create(
                userKey = "test-user-key-$index-${UUID.randomUUID()}",
                name = "테스트 사용자 $index",
                email = "test-user-$index-${UUID.randomUUID()}@coupon.local",
                password = "password",
                role = role,
            ),
        )

    private fun authenticationOf(user: User) =
        UsernamePasswordAuthenticationToken(
            user.key,
            null,
            listOf(SimpleGrantedAuthority(user.role.name)),
        )
}
