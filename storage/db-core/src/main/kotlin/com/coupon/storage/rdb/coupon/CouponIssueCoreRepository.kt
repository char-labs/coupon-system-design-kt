package com.coupon.storage.rdb.coupon

import com.coupon.coupon.CouponIssue
import com.coupon.coupon.CouponIssueDetail
import com.coupon.coupon.CouponIssueRepository
import com.coupon.coupon.criteria.CouponIssueCriteria
import com.coupon.storage.rdb.support.findByIdOrElseThrow
import com.coupon.support.page.OffsetPageRequest
import com.coupon.support.page.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class CouponIssueCoreRepository(
    private val couponIssueJpaRepository: CouponIssueJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
) : CouponIssueRepository {
    override fun save(criteria: CouponIssueCriteria.Create): CouponIssue =
        couponIssueJpaRepository
            .save(CouponIssueEntity(criteria))
            .toCouponIssue()

    override fun findById(couponIssueId: Long): CouponIssue =
        couponIssueJpaRepository
            .findByIdOrElseThrow(couponIssueId)
            .toCouponIssue()

    override fun findDetailById(couponIssueId: Long): CouponIssueDetail {
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
    ): Page<CouponIssueDetail> {
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
    ): Page<CouponIssueDetail> {
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

    override fun use(couponIssueId: Long) {
        val entity = couponIssueJpaRepository.findByIdOrElseThrow(couponIssueId)
        entity.use()
    }

    override fun cancel(couponIssueId: Long) {
        val entity = couponIssueJpaRepository.findByIdOrElseThrow(couponIssueId)
        entity.cancel()
    }
}
