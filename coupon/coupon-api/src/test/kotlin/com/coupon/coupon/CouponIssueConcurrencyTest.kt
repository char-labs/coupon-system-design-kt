package com.coupon.coupon

import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.enums.coupon.CouponIssueStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.testing.CouponApiConcurrencyTest
import com.coupon.support.testing.DatabaseCleaner
import com.coupon.user.User
import com.coupon.user.UserService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.shouldBe
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@CouponApiConcurrencyTest
open class CouponIssueConcurrencyTest(
    private val couponService: CouponService,
    private val couponIssueService: CouponIssueService,
    private val userService: UserService,
    private val databaseCleaner: DatabaseCleaner,
) : BehaviorSpec() {
    init {
        beforeTest {
            databaseCleaner.clean()
        }

        given("쿠폰 발급 동시성 제어") {
            `when`("같은 사용자가 같은 쿠폰을 동시에 발급하면") {
                then("한 번만 성공하고 중복 발급은 차단된다") {
                    val coupon = createCoupon(totalQuantity = 10L)
                    val user = createUser(index = 1)
                    val result =
                        runConcurrently(
                            actions =
                                List(10) {
                                    {
                                        couponIssueService.issueCoupon(
                                            CouponIssueCommand.Issue(couponId = coupon.id, userId = user.id),
                                        )
                                    }
                                },
                        )

                    result.unexpectedErrors.shouldBeEmpty()
                    result.successCount shouldBe 1
                    result.errorTypes.size shouldBe 9
                    result.errorTypes.all { it == ErrorType.ALREADY_ISSUED_COUPON } shouldBe true
                    couponIssueCount(coupon.id) shouldBe 1L
                    couponService.getCoupon(coupon.id).remainingQuantity shouldBe 9L
                }
            }

            `when`("여러 사용자가 수량보다 많이 동시에 발급하면") {
                then("재고 수량만큼만 성공한다") {
                    val totalQuantity = 5L
                    val coupon = createCoupon(totalQuantity = totalQuantity)
                    val users = (1..20).map(::createUser)
                    val result =
                        runConcurrently(
                            actions =
                                users.map {
                                    {
                                        couponIssueService.issueCoupon(
                                            CouponIssueCommand.Issue(couponId = coupon.id, userId = it.id),
                                        )
                                    }
                                },
                        )

                    result.unexpectedErrors.shouldBeEmpty()
                    result.successCount shouldBe totalQuantity.toInt()
                    result.errorTypes.size shouldBe users.size - totalQuantity.toInt()
                    result.errorTypes.all { it == ErrorType.COUPON_OUT_OF_STOCK } shouldBe true
                    couponIssueCount(coupon.id) shouldBe totalQuantity
                    couponService.getCoupon(coupon.id).remainingQuantity.shouldBeZero()
                }
            }
        }

        given("발급된 쿠폰의 상태 전이 동시성 제어") {
            `when`("같은 쿠폰 사용 요청이 동시에 들어오면") {
                then("한 번만 성공한다") {
                    val fixture = createIssuedCouponFixture()
                    val result =
                        runConcurrently(
                            actions =
                                List(10) {
                                    {
                                        couponIssueService.useCoupon(
                                            CouponIssueCommand.Use(
                                                couponIssueId = fixture.couponIssue.id,
                                                userId = fixture.user.id,
                                            ),
                                        )
                                    }
                                },
                        )

                    result.unexpectedErrors.shouldBeEmpty()
                    result.successCount shouldBe 1
                    result.errorTypes.size shouldBe 9
                    result.errorTypes.all { it == ErrorType.INVALID_COUPON_STATUS } shouldBe true
                    couponIssueService.getCouponIssue(fixture.couponIssue.id).status shouldBe CouponIssueStatus.USED
                    couponService.getCoupon(fixture.coupon.id).remainingQuantity.shouldBeZero()
                }
            }

            `when`("같은 쿠폰 취소 요청이 동시에 들어오면") {
                then("한 번만 성공한다") {
                    val fixture = createIssuedCouponFixture()
                    val result =
                        runConcurrently(
                            actions =
                                List(10) {
                                    {
                                        couponIssueService.cancelCoupon(
                                            CouponIssueCommand.Cancel(
                                                couponIssueId = fixture.couponIssue.id,
                                                userId = fixture.user.id,
                                            ),
                                        )
                                    }
                                },
                        )

                    result.unexpectedErrors.shouldBeEmpty()
                    result.successCount shouldBe 1
                    result.errorTypes.size shouldBe 9
                    result.errorTypes.all { it == ErrorType.INVALID_COUPON_STATUS } shouldBe true
                    couponIssueService.getCouponIssue(fixture.couponIssue.id).status shouldBe CouponIssueStatus.CANCELED
                    couponService.getCoupon(fixture.coupon.id).remainingQuantity shouldBe 1L
                }
            }

            `when`("동시에 사용과 취소 요청이 들어오면") {
                then("하나만 성공하고 상태와 재고가 일관된다") {
                    val fixture = createIssuedCouponFixture()
                    val result =
                        runConcurrently(
                            actions =
                                listOf(
                                    {
                                        couponIssueService.useCoupon(
                                            CouponIssueCommand.Use(
                                                couponIssueId = fixture.couponIssue.id,
                                                userId = fixture.user.id,
                                            ),
                                        )
                                    },
                                    {
                                        couponIssueService.cancelCoupon(
                                            CouponIssueCommand.Cancel(
                                                couponIssueId = fixture.couponIssue.id,
                                                userId = fixture.user.id,
                                            ),
                                        )
                                    },
                                ),
                        )

                    result.unexpectedErrors.shouldBeEmpty()
                    result.successCount shouldBe 1
                    result.errorTypes.size shouldBe 1
                    result.errorTypes.all { it == ErrorType.INVALID_COUPON_STATUS } shouldBe true

                    val finalIssue = couponIssueService.getCouponIssue(fixture.couponIssue.id)
                    val remainingQuantity = couponService.getCoupon(fixture.coupon.id).remainingQuantity

                    setOf(CouponIssueStatus.USED, CouponIssueStatus.CANCELED).contains(finalIssue.status) shouldBe true
                    when (finalIssue.status) {
                        CouponIssueStatus.USED -> remainingQuantity.shouldBeZero()
                        CouponIssueStatus.CANCELED -> remainingQuantity shouldBe 1L
                        else -> error("Unexpected coupon issue status: ${finalIssue.status}")
                    }
                }
            }

            `when`("같은 쿠폰에서 취소와 신규 발급이 동시에 들어오면") {
                then("재고 정합성은 유지된다") {
                    val fixture = createIssuedCouponFixture()
                    val anotherUser = createUser(index = 2)
                    val result =
                        runConcurrently(
                            actions =
                                listOf(
                                    {
                                        couponIssueService.cancelCoupon(
                                            CouponIssueCommand.Cancel(
                                                couponIssueId = fixture.couponIssue.id,
                                                userId = fixture.user.id,
                                            ),
                                        )
                                    },
                                    {
                                        couponIssueService.issueCoupon(
                                            CouponIssueCommand.Issue(
                                                couponId = fixture.coupon.id,
                                                userId = anotherUser.id,
                                            ),
                                        )
                                    },
                                ),
                        )

                    result.unexpectedErrors.shouldBeEmpty()
                    result.errorTypes.all { it == ErrorType.COUPON_OUT_OF_STOCK } shouldBe true

                    val couponIssues =
                        couponIssueService.getCouponIssues(
                            fixture.coupon.id,
                            OffsetPageRequest(0, 100),
                        )
                    val activeIssuedCount = couponIssues.content.count { it.status == CouponIssueStatus.ISSUED }
                    val canceledCount = couponIssues.content.count { it.status == CouponIssueStatus.CANCELED }
                    val remainingQuantity = couponService.getCoupon(fixture.coupon.id).remainingQuantity

                    canceledCount shouldBe 1
                    (activeIssuedCount.toLong() + remainingQuantity) shouldBe 1L
                    (remainingQuantity in 0L..1L) shouldBe true
                    setOf(1, 2).contains(result.successCount) shouldBe true
                }
            }
        }
    }

    private fun createCoupon(totalQuantity: Long): Coupon =
        couponService.createCoupon(CouponApiFixtures.couponCreateCommand(totalQuantity = totalQuantity))

    private fun createUser(index: Int): User = userService.createUser(CouponApiFixtures.userCreateCommand(index = index))

    private fun couponIssueCount(couponId: Long): Long = couponIssueService.getCouponIssues(couponId, OffsetPageRequest(0, 100)).totalCount

    private fun createIssuedCouponFixture(): IssuedCouponFixture {
        val coupon = createCoupon(totalQuantity = 1L)
        val user = createUser(index = 1)
        val couponIssue = couponIssueService.issueCoupon(CouponIssueCommand.Issue(couponId = coupon.id, userId = user.id))

        return IssuedCouponFixture(
            coupon = coupon,
            user = user,
            couponIssue = couponIssue,
        )
    }

    private fun runConcurrently(actions: List<() -> Unit>): ConcurrentExecutionResult {
        val readyLatch = CountDownLatch(actions.size)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(actions.size)
        val executor = Executors.newFixedThreadPool(actions.size)
        val successCount = AtomicInteger(0)
        val errorTypes = ConcurrentLinkedQueue<ErrorType>()
        val unexpectedErrors = ConcurrentLinkedQueue<Throwable>()

        try {
            actions.forEach { action ->
                executor.submit {
                    readyLatch.countDown()
                    startLatch.await()
                    try {
                        action()
                        successCount.incrementAndGet()
                    } catch (exception: ErrorException) {
                        errorTypes.add(exception.errorType)
                    } catch (exception: Throwable) {
                        unexpectedErrors.add(exception)
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            readyLatch.await(5, TimeUnit.SECONDS) shouldBe true
            startLatch.countDown()
            doneLatch.await(10, TimeUnit.SECONDS) shouldBe true
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS) shouldBe true

            return ConcurrentExecutionResult(
                successCount = successCount.get(),
                errorTypes = errorTypes.toList(),
                unexpectedErrors = unexpectedErrors.toList(),
            )
        } finally {
            executor.shutdownNow()
        }
    }

    private data class ConcurrentExecutionResult(
        val successCount: Int,
        val errorTypes: List<ErrorType>,
        val unexpectedErrors: List<Throwable>,
    )

    private data class IssuedCouponFixture(
        val coupon: Coupon,
        val user: User,
        val couponIssue: CouponIssue,
    )
}
