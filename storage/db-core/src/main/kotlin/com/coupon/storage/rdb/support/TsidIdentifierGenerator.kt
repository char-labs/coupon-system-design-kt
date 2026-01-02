package com.coupon.storage.rdb.support

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.IdentifierGenerator

/**
 * Hibernate용 TSID ID Generator
 *
 * JPA 엔티티의 ID를 TSID로 자동 생성합니다.
 * @GeneratedValue와 함께 사용하여 엔티티 저장 시 자동으로 TSID를 생성합니다.
 *
 * 사용 예시:
 * ```
 * @Id
 * @GeneratedValue(generator = "tsid")
 * @GenericGenerator(name = "tsid", strategy = "com.coupon.storage.rdb.support.TsidIdentifierGenerator")
 * val id: Long? = null
 * ```
 */
class TsidIdentifierGenerator : IdentifierGenerator {
    override fun generate(
        session: SharedSessionContractImplementor?,
        obj: Any?,
    ): Long = TsidGenerator.generate()
}
