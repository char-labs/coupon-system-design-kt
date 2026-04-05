package com.coupon.storage.rdb.coupon

import com.coupon.coupon.CouponIssue
import com.coupon.coupon.CouponIssueRepository
import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.enums.coupon.CouponIssueStatus
import com.coupon.enums.error.ErrorType
import com.coupon.error.ErrorException
import com.coupon.storage.rdb.support.findByIdOrElseThrow
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
class CouponIssueCoreRepository(
    private val couponIssueJpaRepository: CouponIssueJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
) : CouponIssueRepository {
    @Transactional
    override fun save(criteria: CouponIssueCriteria.Create): CouponIssue =
        try {
            couponIssueJpaRepository
                .saveAndFlush(CouponIssueEntity(criteria))
                .toCouponIssue()
        } catch (exception: DataIntegrityViolationException) {
            if (isDuplicateIssueConstraintViolation(exception)) {
                throw ErrorException(ErrorType.ALREADY_ISSUED_COUPON)
            }
            throw exception
        }

    override fun findById(couponIssueId: Long): CouponIssue =
        couponIssueJpaRepository
            .findByIdOrElseThrow(couponIssueId)
            .toCouponIssue()

    override fun findDetailById(couponIssueId: Long): CouponIssue.Detail {
        val issueEntity = couponIssueJpaRepository.findByIdOrElseThrow(couponIssueId)
        val coupon = couponJpaRepository.findByIdOrElseThrow(issueEntity.couponId)
        return issueEntity.toCouponIssueDetail(
            couponCode = coupon.couponCode,
            couponName = coupon.name,
        )
    }

    override fun findAllByUserId(
        userId: Long,
        request: OffsetPageRequest,
    ): Page<CouponIssue.Detail> {
        val pageable = PageRequest.of(request.page, request.size)
        val result = couponIssueJpaRepository.findAllByUserId(userId, pageable)

        val couponIds = result.content.map { it.couponId }.distinct()
        val coupons = couponJpaRepository.findAllById(couponIds).associateBy { it.id!! }

        return Page(
            content =
                result.content.map { issue ->
                    val coupon = coupons[issue.couponId]!!
                    issue.toCouponIssueDetail(
                        couponCode = coupon.couponCode,
                        couponName = coupon.name,
                    )
                },
            totalCount = result.totalElements,
        )
    }

    override fun findAllByCouponId(
        couponId: Long,
        request: OffsetPageRequest,
    ): Page<CouponIssue.Detail> {
        val pageable = PageRequest.of(request.page, request.size)
        val result = couponIssueJpaRepository.findAllByCouponId(couponId, pageable)
        val coupon = couponJpaRepository.findByIdOrElseThrow(couponId)

        return Page(
            content =
                result.content.map { issue ->
                    issue.toCouponIssueDetail(
                        couponCode = coupon.couponCode,
                        couponName = coupon.name,
                    )
                },
            totalCount = result.totalElements,
        )
    }

    override fun existsByUserIdAndCouponId(
        userId: Long,
        couponId: Long,
    ): Boolean = couponIssueJpaRepository.existsByUserIdAndCouponId(userId, couponId)

    override fun useIfIssued(couponIssueId: Long): Boolean =
        couponIssueJpaRepository.useIfIssued(
            couponIssueId = couponIssueId,
            issuedStatus = CouponIssueStatus.ISSUED,
            usedStatus = CouponIssueStatus.USED,
            usedAt = LocalDateTime.now(),
        ) > 0

    override fun cancelIfIssued(couponIssueId: Long): Boolean =
        couponIssueJpaRepository.cancelIfIssued(
            couponIssueId = couponIssueId,
            issuedStatus = CouponIssueStatus.ISSUED,
            canceledStatus = CouponIssueStatus.CANCELED,
            canceledAt = LocalDateTime.now(),
        ) > 0

    private fun isDuplicateIssueConstraintViolation(exception: DataIntegrityViolationException): Boolean =
        generateSequence<Throwable>(exception) { it.cause }
            .mapNotNull { it.message }
            .any { message ->
                message.contains("uk_coupon_issue_user_coupon", ignoreCase = true) ||
                    message.contains("t_coupon_issue", ignoreCase = true) &&
                    message.contains("user_id", ignoreCase = true) &&
                    message.contains("coupon_id", ignoreCase = true)
            }
}
