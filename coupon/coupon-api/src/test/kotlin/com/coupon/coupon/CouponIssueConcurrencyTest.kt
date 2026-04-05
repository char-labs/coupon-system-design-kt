package com.coupon.coupon

import com.coupon.CouponServerApplication
import com.coupon.coupon.command.CouponCommand
import com.coupon.coupon.command.CouponIssueCommand
import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.enums.CouponIssueStatus
import com.coupon.enums.CouponType
import com.coupon.enums.ErrorType
import com.coupon.error.ErrorException
import com.coupon.storage.rdb.coupon.CouponIssueJpaRepository
import com.coupon.storage.rdb.coupon.CouponJpaRepository
import com.coupon.storage.rdb.user.UserJpaRepository
import com.coupon.support.lock.LockRepository
import com.coupon.user.User
import com.coupon.user.UserService
import com.coupon.user.command.UserCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

@SpringBootTest(
    classes = [CouponServerApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "management.health.redis.enabled=false",
        "spring.main.web-application-type=none",
        "datasource.db.core.driver-class-name=org.h2.Driver",
        "datasource.db.core.jdbc-url=jdbc:h2:mem:coupon-concurrency;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "datasource.db.core.username=sa",
        "datasource.db.core.password=",
        "datasource.db.core.maximum-pool-size=30",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    ],
)
@ActiveProfiles("test")
@Import(CouponIssueConcurrencyTest.TestLockConfig::class)
class CouponIssueConcurrencyTest {
    @Autowired
    private lateinit var couponService: CouponService

    @Autowired
    private lateinit var couponIssueService: CouponIssueService

    @Autowired
    private lateinit var couponIssueRepository: CouponIssueRepository

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var couponJpaRepository: CouponJpaRepository

    @Autowired
    private lateinit var couponIssueJpaRepository: CouponIssueJpaRepository

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        couponIssueJpaRepository.deleteAllInBatch()
        couponJpaRepository.deleteAllInBatch()
        userJpaRepository.deleteAllInBatch()
    }

    @Test
    fun `같은 사용자의 동시 쿠폰 발급 요청은 한 번만 성공한다`() {
        val coupon = createCoupon(totalQuantity = 10)
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

        assertThat(result.unexpectedErrors).isEmpty()
        assertThat(result.successCount).isEqualTo(1)
        assertThat(result.errorTypes).hasSize(9).allMatch { it == ErrorType.ALREADY_ISSUED_COUPON }
        assertThat(couponIssueJpaRepository.count()).isEqualTo(1)
        assertThat(couponService.getCoupon(coupon.id).remainingQuantity).isEqualTo(9)
    }

    @Test
    fun `여러 사용자가 수량보다 많이 동시에 발급하면 재고 수량만큼만 성공한다`() {
        val totalQuantity = 5L
        val coupon = createCoupon(totalQuantity = totalQuantity)
        val users = (1..20).map(::createUser)

        val result =
            runConcurrently(
                actions = users.map { { couponIssueService.issueCoupon(CouponIssueCommand.Issue(couponId = coupon.id, userId = it.id)) } },
            )

        assertThat(result.unexpectedErrors).isEmpty()
        assertThat(result.successCount).isEqualTo(totalQuantity.toInt())
        assertThat(result.errorTypes).hasSize(users.size - totalQuantity.toInt()).allMatch {
            it == ErrorType.COUPON_OUT_OF_STOCK
        }
        assertThat(couponIssueJpaRepository.count()).isEqualTo(totalQuantity)
        assertThat(couponService.getCoupon(coupon.id).remainingQuantity).isZero()
    }

    @Test
    fun `중복 발급 저장은 유니크 제약으로 최종 차단된다`() {
        val coupon = createCoupon(totalQuantity = 10)
        val user = createUser(index = 1)
        val criteria = CouponIssueCriteria.Create(couponId = coupon.id, userId = user.id)

        couponIssueRepository.save(criteria)

        val exception =
            org.junit.jupiter.api.assertThrows<ErrorException> {
                couponIssueRepository.save(criteria)
            }

        assertThat(exception.errorType).isEqualTo(ErrorType.ALREADY_ISSUED_COUPON)
        assertThat(couponIssueJpaRepository.count()).isEqualTo(1)
    }

    @Test
    fun `같은 쿠폰 사용 요청은 한 번만 성공한다`() {
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

        assertThat(result.unexpectedErrors).isEmpty()
        assertThat(result.successCount).isEqualTo(1)
        assertThat(result.errorTypes).hasSize(9).allMatch { it == ErrorType.INVALID_COUPON_STATUS }
        assertThat(couponIssueService.getCouponIssue(fixture.couponIssue.id).status).isEqualTo(CouponIssueStatus.USED)
        assertThat(couponService.getCoupon(fixture.coupon.id).remainingQuantity).isZero()
    }

    @Test
    fun `같은 쿠폰 취소 요청은 한 번만 성공한다`() {
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

        assertThat(result.unexpectedErrors).isEmpty()
        assertThat(result.successCount).isEqualTo(1)
        assertThat(result.errorTypes).hasSize(9).allMatch { it == ErrorType.INVALID_COUPON_STATUS }
        assertThat(couponIssueService.getCouponIssue(fixture.couponIssue.id).status).isEqualTo(CouponIssueStatus.CANCELED)
        assertThat(couponService.getCoupon(fixture.coupon.id).remainingQuantity).isEqualTo(1)
    }

    @Test
    fun `동시에 사용과 취소 요청이 들어오면 하나만 성공한다`() {
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

        assertThat(result.unexpectedErrors).isEmpty()
        assertThat(result.successCount).isEqualTo(1)
        assertThat(result.errorTypes).hasSize(1).allMatch { it == ErrorType.INVALID_COUPON_STATUS }

        val finalIssue = couponIssueService.getCouponIssue(fixture.couponIssue.id)
        val remainingQuantity = couponService.getCoupon(fixture.coupon.id).remainingQuantity

        assertThat(finalIssue.status).isIn(CouponIssueStatus.USED, CouponIssueStatus.CANCELED)
        when (finalIssue.status) {
            CouponIssueStatus.USED -> assertThat(remainingQuantity).isZero()
            CouponIssueStatus.CANCELED -> assertThat(remainingQuantity).isEqualTo(1)
            else -> error("Unexpected coupon issue status: ${finalIssue.status}")
        }
    }

    @Test
    fun `같은 쿠폰에서 취소와 신규 발급이 동시에 들어와도 재고는 깨지지 않는다`() {
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

        assertThat(result.unexpectedErrors).isEmpty()
        assertThat(result.errorTypes).allMatch { it == ErrorType.COUPON_OUT_OF_STOCK }

        val couponIssues =
            couponIssueService.getCouponIssues(
                fixture.coupon.id,
                com.coupon.support.page
                    .OffsetPageRequest(0, 100),
            )
        val activeIssuedCount = couponIssues.content.count { it.status == CouponIssueStatus.ISSUED }
        val canceledCount = couponIssues.content.count { it.status == CouponIssueStatus.CANCELED }
        val remainingQuantity = couponService.getCoupon(fixture.coupon.id).remainingQuantity

        assertThat(canceledCount).isEqualTo(1)
        assertThat(activeIssuedCount + remainingQuantity).isEqualTo(1)
        assertThat(remainingQuantity).isBetween(0, 1)
        assertThat(result.successCount).isIn(1, 2)
    }

    private fun createCoupon(totalQuantity: Long): Coupon =
        couponService.createCoupon(
            CouponCommand.Create(
                name = "Concurrency Coupon ${UUID.randomUUID()}",
                couponType = CouponType.FIXED,
                discountAmount = 1000,
                maxDiscountAmount = null,
                minOrderAmount = null,
                totalQuantity = totalQuantity,
                availableAt = LocalDateTime.now().minusMinutes(5),
                endAt = LocalDateTime.now().plusHours(1),
            ),
        )

    private fun createUser(index: Int): User =
        userService.createUser(
            UserCommand.Create(
                name = "user-$index",
                email = "user-$index-${UUID.randomUUID()}@coupon.local",
                password = "password",
            ),
        )

    private fun createIssuedCouponFixture(): IssuedCouponFixture {
        val coupon = createCoupon(totalQuantity = 1)
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

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue()
        startLatch.countDown()
        assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()

        return ConcurrentExecutionResult(
            successCount = successCount.get(),
            errorTypes = errorTypes.toList(),
            unexpectedErrors = unexpectedErrors.toList(),
        )
    }

    data class ConcurrentExecutionResult(
        val successCount: Int,
        val errorTypes: List<ErrorType>,
        val unexpectedErrors: List<Throwable>,
    )

    data class IssuedCouponFixture(
        val coupon: Coupon,
        val user: User,
        val couponIssue: CouponIssue,
    )

    @TestConfiguration
    class TestLockConfig {
        @Bean
        @Primary
        fun lockRepository(): LockRepository = InMemoryLockRepository()

        @Bean
        fun corsConfigurationSource(): CorsConfigurationSource =
            UrlBasedCorsConfigurationSource().apply {
                registerCorsConfiguration(
                    "/**",
                    CorsConfiguration().apply {
                        allowedOriginPatterns = listOf("*")
                        allowedMethods = listOf("*")
                        allowedHeaders = listOf("*")
                        allowCredentials = true
                    },
                )
            }
    }

    class InMemoryLockRepository : LockRepository {
        private val locks = java.util.concurrent.ConcurrentHashMap<String, ReentrantLock>()

        override fun tryLock(
            key: String,
            timeoutMillis: Long,
        ): Boolean = locks.computeIfAbsent(key) { ReentrantLock() }.tryLock(timeoutMillis, TimeUnit.MILLISECONDS)

        override fun unlock(key: String) {
            val lock = locks[key] ?: return
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }

        override fun <T> executeWithLock(
            key: String,
            timeoutMillis: Long,
            timeoutException: ErrorType,
            func: () -> T,
        ): T {
            val lockSuccess = tryLock(key, timeoutMillis)
            if (!lockSuccess) {
                throw ErrorException(timeoutException)
            }

            try {
                return func()
            } finally {
                unlock(key)
            }
        }
    }
}
