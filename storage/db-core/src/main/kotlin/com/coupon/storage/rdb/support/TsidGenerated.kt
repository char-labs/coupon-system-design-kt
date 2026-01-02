package com.coupon.storage.rdb.support

import org.hibernate.annotations.IdGeneratorType

/**
 * TSID 생성 어노테이션
 *
 * 엔티티의 ID 필드에 이 어노테이션을 적용하면 TSID로 자동 생성됩니다.
 * Hibernate 6+ 권장 방식을 사용합니다.
 *
 * 사용 예시:
 * ```
 * @Id
 * @TsidGenerated
 * val id: Long? = null
 * ```
 */
@IdGeneratorType(TsidIdentifierGenerator::class)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class TsidGenerated
