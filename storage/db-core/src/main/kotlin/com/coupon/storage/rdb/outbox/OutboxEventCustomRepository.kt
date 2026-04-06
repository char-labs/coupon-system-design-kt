package com.coupon.storage.rdb.outbox

import com.coupon.storage.rdb.support.JDSLExtensions
import com.coupon.support.outbox.OutboxEventStatus
import com.linecorp.kotlinjdsl.dsl.jpql.jpql
import com.linecorp.kotlinjdsl.render.RenderContext
import com.linecorp.kotlinjdsl.support.spring.data.jpa.extension.createQuery
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class OutboxEventCustomRepository(
    private val entityManager: EntityManager,
    private val jdslRenderContext: RenderContext,
) {
    fun findProcessable(
        statuses: Set<OutboxEventStatus>,
        availableAt: LocalDateTime,
        pageable: Pageable,
    ): List<OutboxEventEntity> {
        val query =
            jpql(JDSLExtensions) {
                selectFrom(OutboxEventEntity::class)
                    .whereAnd(
                        path(OutboxEventEntity::status).`in`(statuses),
                        path(OutboxEventEntity::availableAt).lessThanOrEqualTo(availableAt),
                    ).orderBy(
                        path(OutboxEventEntity::availableAt).asc(),
                        path(OutboxEventEntity::id).asc(),
                    )
            }

        return entityManager
            .createQuery(query, jdslRenderContext)
            .setFirstResult(pageable.offset.toInt())
            .setMaxResults(pageable.pageSize)
            .resultList
    }
}
