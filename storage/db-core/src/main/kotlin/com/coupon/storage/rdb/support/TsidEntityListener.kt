package com.coupon.storage.rdb.support

import jakarta.persistence.PrePersist
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * TSID 자동 생성을 위한 JPA EntityListener
 *
 * 엔티티가 persist되기 전에 ID가 null이면 TSID를 자동으로 생성하여 할당합니다.
 * Kotlin의 불변 프로퍼티(val)에도 Reflection을 통해 값을 설정합니다.
 */
class TsidEntityListener {
    @PrePersist
    fun prePersist(entity: Any) {
        val idProperty =
            entity::class
                .memberProperties
                .firstOrNull { it.name == "id" }
                ?: return

        // 현재 ID 값 확인
        idProperty.isAccessible = true
        val currentId = idProperty.getter.call(entity) as? Long

        // ID가 null인 경우에만 TSID 생성
        if (currentId == null) {
            val javaField = idProperty.javaField ?: return
            javaField.isAccessible = true

            // TSID 생성 및 할당
            val tsid = TsidGenerator.generate()
            println("Generated TSID: $tsid for entity: ${entity::class.simpleName}")
            javaField.set(entity, tsid)
        }
    }
}
