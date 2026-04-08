package com.coupon.coupon

import com.coupon.coupon.execution.CouponIssueCancellationExecutor
import com.coupon.coupon.execution.CouponIssueExecutionFacade
import com.coupon.coupon.execution.CouponIssueExecutionResult.Rejected
import com.coupon.coupon.execution.CouponIssueLockingExecutor
import com.coupon.coupon.fixture.CouponIssueFixtures
import com.coupon.coupon.fixture.FixedCouponFixtures
import com.coupon.coupon.intake.CouponIssueIntakeFacade
import com.coupon.coupon.intake.CouponIssueMessage
import com.coupon.coupon.intake.CouponIssueMessagePublisher
import com.coupon.coupon.intake.CouponIssuePublishReceipt
import com.coupon.coupon.support.LockExecution
import com.coupon.coupon.support.RecordingLockRepository
import com.coupon.enums.coupon.CouponIssueResult
import com.coupon.enums.coupon.CouponIssueStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.shared.lock.DistributedLockAspect
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory
import java.time.Clock

class CouponIssueExecutionFacadeTest :
    BehaviorSpec({
        given("CouponIssueExecutionFacade로 발급 요청을 처리하면") {
            `when`("쿠폰 조회, Redis 선점, Kafka 발행이 모두 성공하면") {
                val context = CouponIssueExecutionFacadeTestContext()
                val command = CouponIssueFixtures.issueCommand(couponId = 12L, userId = 120L)
                val coupon = FixedCouponFixtures.standard(id = command.couponId, totalQuantity = 10L)

                every { context.couponService.getAvailableCouponForIssue(command.couponId) } returns coupon
                every { context.couponIssueService.reserveIssue(coupon, command.userId) } returns CouponIssueResult.SUCCESS
                every {
                    context.couponIssueMessagePublisher.publish(
                        match {
                            it.couponId == command.couponId && it.userId == command.userId
                        },
                    )
                } returns CouponIssuePublishReceipt(topic = "coupon.issue.v1", partition = 0, offset = 1L)

                val result = context.couponIssueIntakeFacade.issue(command)

                then("발급 성공을 반환한다") {
                    result shouldBe CouponIssueResult.SUCCESS
                    verifySequence {
                        context.couponService.getAvailableCouponForIssue(command.couponId)
                        context.couponIssueService.reserveIssue(coupon, command.userId)
                        context.couponIssueMessagePublisher.publish(
                            match {
                                it.couponId == command.couponId && it.userId == command.userId
                            },
                        )
                    }
                }
            }

            `when`("Redis 선점 결과가 DUPLICATE면") {
                val context = CouponIssueExecutionFacadeTestContext()
                val command = CouponIssueFixtures.issueCommand(couponId = 13L, userId = 130L)
                val coupon = FixedCouponFixtures.standard(id = command.couponId, totalQuantity = 10L)

                every { context.couponService.getAvailableCouponForIssue(command.couponId) } returns coupon
                every { context.couponIssueService.reserveIssue(coupon, command.userId) } returns CouponIssueResult.DUPLICATE

                val result = context.couponIssueIntakeFacade.issue(command)

                then("즉시 duplicate를 반환하고 발행은 생략한다") {
                    result shouldBe CouponIssueResult.DUPLICATE
                    verify(exactly = 0) { context.couponIssueMessagePublisher.publish(any()) }
                }
            }

            `when`("Kafka publish가 실패하면") {
                val context = CouponIssueExecutionFacadeTestContext()
                val command = CouponIssueFixtures.issueCommand(couponId = 14L, userId = 140L)
                val coupon = FixedCouponFixtures.standard(id = command.couponId, totalQuantity = 10L)

                every { context.couponService.getAvailableCouponForIssue(command.couponId) } returns coupon
                every { context.couponIssueService.reserveIssue(coupon, command.userId) } returns CouponIssueResult.SUCCESS
                every {
                    context.couponIssueMessagePublisher.publish(
                        match {
                            it.couponId == command.couponId && it.userId == command.userId
                        },
                    )
                } throws RuntimeException("broker timeout")
                justRun { context.couponIssueService.release(command.couponId, command.userId) }

                val exception =
                    io.kotest.assertions.throwables.shouldThrow<ErrorException> {
                        context.couponIssueIntakeFacade.issue(command)
                    }

                then("Redis reserve를 해제하고 Kafka 오류를 반환한다") {
                    exception.errorType shouldBe ErrorType.COUPON_ISSUE_KAFKA_ERROR
                    verifySequence {
                        context.couponService.getAvailableCouponForIssue(command.couponId)
                        context.couponIssueService.reserveIssue(coupon, command.userId)
                        context.couponIssueMessagePublisher.publish(
                            match {
                                it.couponId == command.couponId && it.userId == command.userId
                            },
                        )
                        context.couponIssueService.release(command.couponId, command.userId)
                    }
                }
            }
        }

        given("CouponIssueExecutionFacade로 비동기 발급을 실행하면") {
            `when`("쿠폰 검증과 재고 차감이 모두 성공하면") {
                val context = CouponIssueExecutionFacadeTestContext()
                val command = CouponIssueFixtures.issueCommand(couponId = 10L, userId = 100L)
                val issuedCoupon = CouponIssueFixtures.issued(id = 1L, couponId = command.couponId, userId = command.userId)

                justRun { context.couponService.validateAvailability(command.couponId) }
                every { context.couponService.decreaseQuantityIfAvailable(command.couponId) } returns true
                every { context.couponIssueService.executeIssue(command) } returns issuedCoupon

                val result = context.couponIssueExecutionFacade.executeIssue(command)

                then("락 안에서 쿠폰 검증, 재고 차감, 발급 저장을 순서대로 수행한다") {
                    result shouldBe issuedCoupon
                    context.recordedLockExecutions.single() shouldBe
                        LockExecution(
                            key = "COUPON_ISSUE:${command.couponId}",
                            timeoutMillis = 5_000L,
                            timeoutException = ErrorType.LOCK_ACQUISITION_FAILED,
                        )
                    verifySequence {
                        context.couponService.validateAvailability(command.couponId)
                        context.couponService.decreaseQuantityIfAvailable(command.couponId)
                        context.couponIssueService.executeIssue(command)
                    }
                }
            }

            `when`("재고 차감이 실패하면") {
                val context = CouponIssueExecutionFacadeTestContext()
                val message = CouponIssueMessage(couponId = 11L, userId = 101L)

                justRun { context.couponService.validateAvailability(message.couponId) }
                every { context.couponService.decreaseQuantityIfAvailable(message.couponId) } returns false
                justRun { context.couponIssueService.release(message.couponId, message.userId) }

                val result = context.couponIssueExecutionFacade.execute(message)

                then("거절 결과를 반환하고 Redis 발급 상태를 해제한다") {
                    result shouldBe Rejected(ErrorType.COUPON_OUT_OF_STOCK)
                    verifySequence {
                        context.couponService.validateAvailability(message.couponId)
                        context.couponService.decreaseQuantityIfAvailable(message.couponId)
                        context.couponIssueService.release(message.couponId, message.userId)
                    }
                }
            }
        }

        given("CouponIssueExecutionFacade로 쿠폰을 취소하면") {
            `when`("취소 가능한 쿠폰이면") {
                val context = CouponIssueExecutionFacadeTestContext()
                val command = CouponIssueFixtures.cancelCommand(couponIssueId = 9L, userId = 100L)
                val issuedDetail =
                    CouponIssueFixtures.detail(
                        id = command.couponIssueId,
                        couponId = 55L,
                        userId = command.userId,
                        status = CouponIssueStatus.ISSUED,
                    )
                val canceledDetail = issuedDetail.copy(status = CouponIssueStatus.CANCELED)

                every { context.couponIssueService.getCouponIssue(command.couponIssueId) } returnsMany listOf(issuedDetail, canceledDetail)
                every {
                    context.couponIssueService.cancelIssue(command)
                } returns CouponIssueFixtures.issued(id = command.couponIssueId, couponId = issuedDetail.couponId, userId = command.userId)
                justRun { context.couponService.increaseQuantity(issuedDetail.couponId) }
                justRun { context.couponIssueService.releaseStockSlot(issuedDetail.couponId) }

                val result = context.couponIssueExecutionFacade.cancelCoupon(command)

                then("취소 후 재고와 Redis 슬롯을 복구하고 상세 정보를 반환한다") {
                    result shouldBe canceledDetail
                    context.recordedLockExecutions.single() shouldBe
                        LockExecution(
                            key = "COUPON_ISSUE:${issuedDetail.couponId}",
                            timeoutMillis = 5_000L,
                            timeoutException = ErrorType.LOCK_ACQUISITION_FAILED,
                        )
                    verifySequence {
                        context.couponIssueService.getCouponIssue(command.couponIssueId)
                        context.couponIssueService.cancelIssue(command)
                        context.couponService.increaseQuantity(issuedDetail.couponId)
                        context.couponIssueService.releaseStockSlot(issuedDetail.couponId)
                        context.couponIssueService.getCouponIssue(command.couponIssueId)
                    }
                }
            }

            `when`("취소 도중 상태 예외가 발생하면") {
                val context = CouponIssueExecutionFacadeTestContext()
                val command = CouponIssueFixtures.cancelCommand(couponIssueId = 10L, userId = 101L)
                val detail = CouponIssueFixtures.detail(id = command.couponIssueId, couponId = 56L, userId = command.userId)

                every { context.couponIssueService.getCouponIssue(command.couponIssueId) } returns detail
                every { context.couponIssueService.cancelIssue(command) } throws ErrorException(ErrorType.INVALID_COUPON_STATUS)

                val exception =
                    io.kotest.assertions.throwables.shouldThrow<ErrorException> {
                        context.couponIssueExecutionFacade.cancelCoupon(command)
                    }

                then("재고 복구와 Redis 슬롯 해제는 수행하지 않는다") {
                    exception.errorType shouldBe ErrorType.INVALID_COUPON_STATUS
                    verify(exactly = 0) { context.couponService.increaseQuantity(any()) }
                    verify(exactly = 0) { context.couponIssueService.releaseStockSlot(any()) }
                }
            }
        }
    }) {
    private class CouponIssueExecutionFacadeTestContext {
        private val lockRepository = RecordingLockRepository()

        private val tx =
            com.coupon.shared.tx
                .RequiresNewTransactionExecutor()
        val couponService = mockk<CouponService>()
        val couponIssueService = mockk<CouponIssueService>()
        val couponIssueMessagePublisher = mockk<CouponIssueMessagePublisher>()
        private val lock =
            com.coupon.shared.lock
                .Lock(
                    com.coupon.shared.lock.Lock
                        .LockExecutor(lockRepository, tx),
                )
        val couponIssueIntakeFacade =
            CouponIssueIntakeFacade(
                couponService = couponService,
                couponIssueService = couponIssueService,
                couponIssueMessagePublisher = couponIssueMessagePublisher,
                clock = Clock.systemUTC(),
            )
        private val distributedLockAspect = DistributedLockAspect(lock)
        private val couponIssueLockingExecutor: CouponIssueLockingExecutor =
            AspectJProxyFactory(
                CouponIssueLockingExecutor(
                    couponService = couponService,
                    couponIssueService = couponIssueService,
                ),
            ).apply {
                addAspect(distributedLockAspect)
            }.getProxy() as CouponIssueLockingExecutor
        private val couponIssueCancellationExecutor: CouponIssueCancellationExecutor =
            AspectJProxyFactory(
                CouponIssueCancellationExecutor(
                    couponService = couponService,
                    couponIssueService = couponIssueService,
                ),
            ).apply {
                addAspect(distributedLockAspect)
            }.getProxy() as CouponIssueCancellationExecutor
        val couponIssueExecutionFacade: CouponIssueExecutionFacade =
            CouponIssueExecutionFacade(
                couponIssueService = couponIssueService,
                couponIssueLockingExecutor = couponIssueLockingExecutor,
                couponIssueCancellationExecutor = couponIssueCancellationExecutor,
            )

        val recordedLockExecutions get() = lockRepository.executions
    }
}
