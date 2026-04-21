package com.coupon.coupon

import com.coupon.coupon.criteria.CouponCriteria
import com.coupon.coupon.fixture.CouponCommandFixtures
import com.coupon.coupon.fixture.CouponFixtures
import com.coupon.coupon.fixture.FixedCouponFixtures
import com.coupon.shared.cache.Cache
import com.coupon.shared.page.OffsetPageRequest
import com.coupon.shared.page.Page
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import java.time.Clock

class CouponServiceTest :
    BehaviorSpec({
        given("CouponService로 쿠폰을 생성하면") {
            `when`("생성 요청이 들어오면") {
                val context = CouponServiceTestContext()
                val command = CouponCommandFixtures.create(name = "신규 쿠폰", discountAmount = 3_000L, totalQuantity = 20L)
                val generatedCode = "20260406_CP_ABCDEF123456"
                val savedCoupon = CouponFixtures.standard(id = 101L, code = generatedCode, name = command.name)
                val criteriaSlot = slot<CouponCriteria.Create>()

                every { context.couponCodeGenerator.generate() } returns generatedCode
                every { context.couponRepository.save(capture(criteriaSlot)) } returns savedCoupon

                val createdCoupon = context.couponService.createCoupon(command)

                then("생성 코드를 포함한 criteria를 저장하고 결과를 반환한다") {
                    createdCoupon shouldBe savedCoupon
                    criteriaSlot.captured shouldBe CouponCriteria.Create.of(generatedCode, command)
                    verifySequence {
                        context.couponCodeGenerator.generate()
                        context.couponRepository.save(any())
                    }
                }
            }
        }

        given("CouponService로 쿠폰 단건 조회를 하면") {
            `when`("쿠폰 id가 주어지면") {
                val context = CouponServiceTestContext()
                val couponDetail = FixedCouponFixtures.standard(id = 11L)

                every { context.couponRepository.findDetailById(couponDetail.id) } returns couponDetail

                val result = context.couponService.getCoupon(couponDetail.id)

                then("repository 결과를 그대로 반환한다") {
                    result shouldBe couponDetail
                    verify(exactly = 1) { context.couponRepository.findDetailById(couponDetail.id) }
                }
            }
        }

        given("CouponService로 쿠폰 목록을 조회하면") {
            `when`("페이지 요청이 주어지면") {
                val context = CouponServiceTestContext()
                val request = OffsetPageRequest(page = 0, size = 20)
                val coupons =
                    Page.of(
                        content = listOf(FixedCouponFixtures.standard(id = 1L), FixedCouponFixtures.standard(id = 2L)),
                        totalCount = 2L,
                    )

                every { context.couponRepository.findAllBy(request) } returns coupons

                val result = context.couponService.getCoupons(request)

                then("repository 페이지 결과를 그대로 반환한다") {
                    result shouldBe coupons
                    verify(exactly = 1) { context.couponRepository.findAllBy(request) }
                }
            }
        }

        given("CouponService로 쿠폰을 수정하면") {
            `when`("수정 요청이 들어오면") {
                val context = CouponServiceTestContext()
                val couponId = 77L
                val command = CouponCommandFixtures.update(name = "수정된 쿠폰", discountAmount = 8_000L)
                val updatedCoupon = FixedCouponFixtures.standard(id = couponId, discountAmount = 8_000L)
                val criteriaSlot = slot<CouponCriteria.Update>()

                every { context.couponRepository.update(couponId, capture(criteriaSlot)) } returns updatedCoupon

                val result = context.couponService.modifyCoupon(couponId, command)

                then("수정 criteria를 만들어 저장하고 결과를 반환한다") {
                    result shouldBe updatedCoupon
                    criteriaSlot.captured shouldBe CouponCriteria.Update.of(command)
                    verify(exactly = 1) { context.couponRepository.update(couponId, any()) }
                }
            }
        }

        given("CouponService로 쿠폰을 활성화하면") {
            `when`("활성화 직후 발급 준비 상태를 미리 채우면") {
                val context = CouponServiceTestContext()
                val couponId = 55L
                val couponDetail = FixedCouponFixtures.standard(id = couponId, totalQuantity = 100L, remainingQuantity = 100L)

                justRun { context.couponRepository.activate(couponId) }
                every { context.couponRepository.findDetailById(couponId) } returns couponDetail
                justRun {
                    context.couponIssueStateInitializationExecutor.initializeStateIfAbsent(
                        couponDetail,
                        any(),
                    )
                }

                context.couponService.activateCoupon(couponId)

                then("발급 캐시와 Redis 상태를 prewarm 한다") {
                    verifySequence {
                        context.couponRepository.activate(couponId)
                        context.cache.evict("coupon:issue:detail:$couponId")
                        context.couponRepository.findDetailById(couponId)
                        context.cache.putValue("coupon:issue:detail:$couponId", couponDetail, 60L)
                        context.couponIssueStateInitializationExecutor.initializeStateIfAbsent(couponDetail, any())
                    }
                }
            }

            `when`("prewarm 중 예외가 나면") {
                val context = CouponServiceTestContext()
                val couponId = 56L
                val couponDetail = FixedCouponFixtures.standard(id = couponId)

                justRun { context.couponRepository.activate(couponId) }
                every { context.couponRepository.findDetailById(couponId) } returns couponDetail
                every {
                    context.couponIssueStateInitializationExecutor.initializeStateIfAbsent(
                        couponDetail,
                        any(),
                    )
                } throws IllegalStateException("boom")

                then("활성화 자체는 실패시키지 않는다") {
                    shouldNotThrowAny {
                        context.couponService.activateCoupon(couponId)
                    }
                    verify(exactly = 1) { context.couponRepository.activate(couponId) }
                }
            }
        }

        listOf(
            CouponMutationCase(
                description = "비활성화 요청이 들어오면",
                execute = { service, couponId -> service.deactivateCoupon(couponId) },
                verify = { repository, couponId -> verify(exactly = 1) { repository.deactivate(couponId) } },
            ),
            CouponMutationCase(
                description = "삭제 요청이 들어오면",
                execute = { service, couponId -> service.deleteCoupon(couponId) },
                verify = { repository, couponId -> verify(exactly = 1) { repository.delete(couponId) } },
            ),
        ).forEach { case ->
            given("CouponService로 쿠폰 상태를 변경하면") {
                `when`(case.description) {
                    val context = CouponServiceTestContext()
                    val couponId = 55L

                    justRun { context.couponRepository.activate(any()) }
                    justRun { context.couponRepository.deactivate(any()) }
                    justRun { context.couponRepository.delete(any()) }

                    case.execute(context.couponService, couponId)

                    then("대상 repository 메서드를 호출한다") {
                        case.verify(context.couponRepository, couponId)
                    }
                }
            }
        }
    }) {
    private class CouponServiceTestContext {
        val couponRepository = mockk<CouponRepository>()
        val couponIssueRepository = mockk<CouponIssueRepository>()
        val couponCodeGenerator = mockk<CouponCodeGenerator>()
        val couponValidator = mockk<CouponValidator>(relaxed = true)
        val cache = mockk<Cache>(relaxed = true)
        val couponIssueStateInitializationExecutor = mockk<CouponIssueStateInitializationExecutor>()
        val couponService =
            CouponService(
                couponRepository = couponRepository,
                couponIssueRepository = couponIssueRepository,
                couponCodeGenerator = couponCodeGenerator,
                couponValidator = couponValidator,
                couponEligibilityEvaluator = CouponEligibilityEvaluator(),
                couponDiscountCalculator = CouponDiscountCalculator(),
                cache = cache,
                couponIssueStateInitializationExecutor = couponIssueStateInitializationExecutor,
                clock = Clock.systemUTC(),
            )
    }

    private data class CouponMutationCase(
        val description: String,
        val execute: (CouponService, Long) -> Unit,
        val verify: (CouponRepository, Long) -> Unit,
    )
}
