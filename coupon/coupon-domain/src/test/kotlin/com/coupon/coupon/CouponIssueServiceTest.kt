package com.coupon.coupon

import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.coupon.event.CouponLifecycleDomainEvent
import com.coupon.coupon.fixture.CouponIssueFixtures
import com.coupon.coupon.fixture.FixedCouponFixtures
import com.coupon.coupon.support.DomainServiceTestRuntime
import com.coupon.coupon.support.LockExecution
import com.coupon.coupon.support.RecordingLockRepository
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.enums.coupon.CouponIssueStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.springframework.context.ApplicationEventPublisher

class CouponIssueServiceTest :
    BehaviorSpec({
        given("CouponIssueService로 Redis 발급 상태를 선점하면") {
            `when`("상태가 아직 초기화되지 않았다면") {
                val context = CouponIssueServiceTestContext()
                val coupon = FixedCouponFixtures.standard(id = 12L, totalQuantity = 10L)

                every { context.couponIssueRedisRepository.hasState(coupon.id) } returnsMany listOf(false, false)
                every { context.couponIssueRepository.findUserIdsByCouponId(coupon.id) } returns setOf(1L, 2L)
                justRun {
                    context.couponIssueRedisRepository.rebuild(
                        couponId = coupon.id,
                        occupiedCount = coupon.totalQuantity - coupon.remainingQuantity,
                        userIds = setOf(1L, 2L),
                        ttl = any(),
                    )
                }
                every {
                    context.couponIssueRedisRepository.reserve(
                        couponId = coupon.id,
                        userId = 120L,
                        totalQuantity = coupon.totalQuantity,
                        ttl = any(),
                    )
                } returns CouponIssueResult.SUCCESS

                val result = context.couponIssueService.reserveIssue(coupon, 120L)

                then("기존 발급 상태를 복구한 뒤 reserve를 수행한다") {
                    result shouldBe CouponIssueResult.SUCCESS
                    context.recordedLockExecutions.single() shouldBe
                        LockExecution(
                            key = "COUPON_ISSUE_STATE_INIT:${coupon.id}",
                            timeoutMillis = 1_000L,
                            timeoutException = ErrorType.LOCK_ACQUISITION_FAILED,
                        )
                    verifySequence {
                        context.couponIssueRedisRepository.hasState(coupon.id)
                        context.couponIssueRedisRepository.hasState(coupon.id)
                        context.couponIssueRepository.findUserIdsByCouponId(coupon.id)
                        context.couponIssueRedisRepository.rebuild(
                            couponId = coupon.id,
                            occupiedCount = coupon.totalQuantity - coupon.remainingQuantity,
                            userIds = setOf(1L, 2L),
                            ttl = any(),
                        )
                        context.couponIssueRedisRepository.reserve(
                            couponId = coupon.id,
                            userId = 120L,
                            totalQuantity = coupon.totalQuantity,
                            ttl = any(),
                        )
                    }
                }
            }
        }

        given("CouponIssueService로 발급 메시지를 발행하면") {
            `when`("쿠폰과 사용자 id가 주어지면") {
                val context = CouponIssueServiceTestContext()

                justRun { context.couponIssueEventPublisher.publish(CouponIssueMessage(10L, 100L)) }

                context.couponIssueService.publishIssue(couponId = 10L, userId = 100L)

                then("발급 이벤트 퍼블리셔를 호출한다") {
                    verify(exactly = 1) { context.couponIssueEventPublisher.publish(CouponIssueMessage(10L, 100L)) }
                }
            }
        }

        given("CouponIssueService로 쿠폰 발급을 저장하면") {
            `when`("검증과 재고 차감이 외부에서 끝난 command가 주어지면") {
                val context = CouponIssueServiceTestContext()
                val command = CouponIssueFixtures.issueCommand(couponId = 10L, userId = 100L)
                val issuedCoupon = CouponIssueFixtures.issued(id = 1L, couponId = command.couponId, userId = command.userId)
                val criteriaSlot = slot<CouponIssueCriteria.Create>()

                every { context.couponIssueRepository.save(capture(criteriaSlot)) } returns issuedCoupon

                val result = context.couponIssueService.executeIssue(command)

                then("발급 row를 저장하고 issued 이벤트를 발행한다") {
                    result shouldBe issuedCoupon
                    criteriaSlot.captured shouldBe CouponIssueCriteria.Create.of(command)
                    verifySequence {
                        context.couponIssueRepository.save(any())
                        context.applicationEventPublisher.publishEvent(any<CouponLifecycleDomainEvent.Issued>())
                    }
                }
            }
        }

        given("CouponIssueService로 발급 쿠폰을 조회하면") {
            `when`("쿠폰 발급 id가 주어지면") {
                val context = CouponIssueServiceTestContext()
                val detail = CouponIssueFixtures.detail(id = 3L)

                every { context.couponIssueRepository.findDetailById(detail.id) } returns detail

                val result = context.couponIssueService.getCouponIssue(detail.id)

                then("상세 정보를 그대로 반환한다") {
                    result shouldBe detail
                    verify(exactly = 1) { context.couponIssueRepository.findDetailById(detail.id) }
                }
            }
        }

        given("CouponIssueService로 사용자 쿠폰 목록을 조회하면") {
            `when`("사용자 id와 페이지 요청이 주어지면") {
                val context = CouponIssueServiceTestContext()
                val request = OffsetPageRequest(page = 0, size = 20)
                val coupons =
                    Page.of(
                        content = listOf(CouponIssueFixtures.detail(id = 1L, userId = 100L)),
                        totalCount = 1L,
                    )

                every { context.couponIssueRepository.findAllByUserId(100L, request) } returns coupons

                val result = context.couponIssueService.getMyCoupons(100L, request)

                then("repository 페이지 결과를 그대로 반환한다") {
                    result shouldBe coupons
                    verify(exactly = 1) { context.couponIssueRepository.findAllByUserId(100L, request) }
                }
            }
        }

        given("CouponIssueService로 쿠폰별 발급 목록을 조회하면") {
            `when`("쿠폰 id와 페이지 요청이 주어지면") {
                val context = CouponIssueServiceTestContext()
                val request = OffsetPageRequest(page = 1, size = 10)
                val coupons =
                    Page.of(
                        content = listOf(CouponIssueFixtures.detail(id = 2L, couponId = 10L)),
                        totalCount = 3L,
                    )

                every { context.couponIssueRepository.findAllByCouponId(10L, request) } returns coupons

                val result = context.couponIssueService.getCouponIssues(10L, request)

                then("repository 페이지 결과를 그대로 반환한다") {
                    result shouldBe coupons
                    verify(exactly = 1) { context.couponIssueRepository.findAllByCouponId(10L, request) }
                }
            }
        }

        given("CouponIssueService로 쿠폰을 사용하면") {
            `when`("소유한 ISSUED 쿠폰이면") {
                val context = CouponIssueServiceTestContext()
                val command = CouponIssueFixtures.useCommand(couponIssueId = 7L, userId = 100L)
                val couponIssue = CouponIssueFixtures.issued(id = command.couponIssueId, couponId = 10L, userId = command.userId)
                val detail =
                    CouponIssueFixtures.detail(
                        id = command.couponIssueId,
                        couponId = 10L,
                        userId = command.userId,
                        status = CouponIssueStatus.USED,
                    )

                every { context.couponIssueRepository.findById(command.couponIssueId) } returns couponIssue
                justRun { context.couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId) }
                every { context.couponIssueRepository.useIfIssued(command.couponIssueId) } returns true
                every { context.couponIssueRepository.findDetailById(command.couponIssueId) } returns detail

                val result = context.couponIssueService.useCoupon(command)

                then("상태 변경 후 상세 정보를 반환한다") {
                    result shouldBe detail
                    context.recordedLockExecutions.single() shouldBe
                        LockExecution(
                            key = "COUPON_ISSUE_STATUS:${command.couponIssueId}",
                            timeoutMillis = 5_000L,
                            timeoutException = ErrorType.LOCK_ACQUISITION_FAILED,
                        )
                    verifySequence {
                        context.couponIssueRepository.findById(command.couponIssueId)
                        context.couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId)
                        context.couponIssueRepository.useIfIssued(command.couponIssueId)
                        context.applicationEventPublisher.publishEvent(any<CouponLifecycleDomainEvent.Used>())
                        context.couponIssueRepository.findDetailById(command.couponIssueId)
                    }
                }
            }

            `when`("이미 사용되었거나 취소된 쿠폰이면") {
                val context = CouponIssueServiceTestContext()
                val command = CouponIssueFixtures.useCommand(couponIssueId = 8L, userId = 101L)
                val couponIssue = CouponIssueFixtures.issued(id = command.couponIssueId, couponId = 10L, userId = command.userId)

                every { context.couponIssueRepository.findById(command.couponIssueId) } returns couponIssue
                justRun { context.couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId) }
                every { context.couponIssueRepository.useIfIssued(command.couponIssueId) } returns false

                val exception =
                    shouldThrow<ErrorException> {
                        context.couponIssueService.useCoupon(command)
                    }

                then("상태 변경 예외를 반환하고 상세 조회는 하지 않는다") {
                    exception.errorType shouldBe ErrorType.INVALID_COUPON_STATUS
                    verify(exactly = 0) { context.couponIssueRepository.findDetailById(command.couponIssueId) }
                    verify(exactly = 0) { context.applicationEventPublisher.publishEvent(any<CouponLifecycleDomainEvent.Used>()) }
                }
            }
        }

        given("CouponIssueService로 쿠폰을 취소하면") {
            `when`("소유한 ISSUED 쿠폰이면") {
                val context = CouponIssueServiceTestContext()
                val command = CouponIssueFixtures.cancelCommand(couponIssueId = 9L, userId = 100L)
                val couponIssue = CouponIssueFixtures.issued(id = command.couponIssueId, couponId = 55L, userId = command.userId)

                every { context.couponIssueRepository.findById(command.couponIssueId) } returns couponIssue
                justRun { context.couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId) }
                every { context.couponIssueRepository.cancelIfIssued(command.couponIssueId) } returns true

                val result = context.couponIssueService.cancelIssue(command)

                then("상태를 취소로 바꾸고 canceled 이벤트를 발행한다") {
                    result shouldBe couponIssue
                    verifySequence {
                        context.couponIssueRepository.findById(command.couponIssueId)
                        context.couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId)
                        context.couponIssueRepository.cancelIfIssued(command.couponIssueId)
                        context.applicationEventPublisher.publishEvent(any<CouponLifecycleDomainEvent.Canceled>())
                    }
                }
            }

            `when`("이미 사용되었거나 취소된 쿠폰이면") {
                val context = CouponIssueServiceTestContext()
                val command = CouponIssueFixtures.cancelCommand(couponIssueId = 10L, userId = 100L)
                val couponIssue = CouponIssueFixtures.issued(id = command.couponIssueId, couponId = 55L, userId = command.userId)

                every { context.couponIssueRepository.findById(command.couponIssueId) } returns couponIssue
                justRun { context.couponIssueValidator.validateOwnedCouponIssue(couponIssue, command.userId) }
                every { context.couponIssueRepository.cancelIfIssued(command.couponIssueId) } returns false

                val exception =
                    shouldThrow<ErrorException> {
                        context.couponIssueService.cancelIssue(command)
                    }

                then("상태 변경 예외를 반환하고 canceled 이벤트는 발행하지 않는다") {
                    exception.errorType shouldBe ErrorType.INVALID_COUPON_STATUS
                    verify(exactly = 0) { context.applicationEventPublisher.publishEvent(any<CouponLifecycleDomainEvent.Canceled>()) }
                }
            }
        }
    }) {
    private class CouponIssueServiceTestContext {
        private val lockRepository = RecordingLockRepository()
        val couponIssueRepository = mockk<CouponIssueRepository>()
        val couponIssueValidator = mockk<CouponIssueValidator>()
        val couponIssueRedisRepository = mockk<CouponIssueRedisRepository>(relaxed = true)
        val couponIssueEventPublisher = mockk<CouponIssueEventPublisher>(relaxed = true)
        val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
        val couponIssueService =
            CouponIssueService(
                couponIssueRepository = couponIssueRepository,
                couponIssueValidator = couponIssueValidator,
                couponIssueRedisRepository = couponIssueRedisRepository,
                couponIssueEventPublisher = couponIssueEventPublisher,
                applicationEventPublisher = applicationEventPublisher,
            )

        val recordedLockExecutions get() = lockRepository.executions

        init {
            DomainServiceTestRuntime.initialize(lockRepository)
        }
    }
}
